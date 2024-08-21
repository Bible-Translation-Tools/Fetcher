import logging
import re
from pathlib import Path
from typing import Dict

from file_utils import init_temp_dir, rm_tree, copy_dir, check_file_exists, copy_file, check_dir_empty, has_new_files, \
    rel_path, read_hash, write_hash
from process_tools import split_chapter, convert_to_mp3
from constants import *
from concurrent.futures import ThreadPoolExecutor
from time import time


class ChapterWorker:

    def __init__(self, input_dir: Path, verbose=False):
        self.__ftp_dir = input_dir
        self.__temp_dir = None

        self.__chapter_regex = r'_c[\d]+\..*$'

        self.verbose = verbose

        self.resources_created = []
        self.resources_deleted = []
        self.thread_executor = ThreadPoolExecutor()

    def execute(self, all_files: set[Path]):
        """ Execute worker """
        start_time = time()
        logging.info("Chapter worker started!")
        self.clear_report()
        self.__temp_dir = init_temp_dir("chapter_worker_")
        self.thread_executor.map(self.process_chapter, all_files)

        logging.debug(f'Deleting temporary directory {self.__temp_dir}')
        rm_tree(self.__temp_dir)
        # remove from this set passed set: 
        all_files.difference_update(set(self.resources_deleted))
        # add anything new for subsequent workers to have in additional to initial fs read
        all_files.update(set(self.resources_created))
        end_time = time()
        logging.info(f"Chapter worker finished in {end_time - start_time} seconds!")
        return 
    
    def include_file(self, file:Path) -> bool:
        return re.search(self.__chapter_regex, str(file))
    
    def process_chapter(self, src_file: Path):
        try:
            if not self.include_file(src_file):
                return
            
            changed = self.check_file_changed(src_file)
            if not changed:
                logging.debug(f'Chapter {src_file} has not been changed. Skipping...')
                return
            # Extract necessary path parts
            logging.debug(f'Found chapter file: {src_file}')
            root_parts = self.__ftp_dir.parts
            parts = src_file.parts[len(root_parts):]

            lang = parts[0]
            resource = parts[1]
            book = parts[2]
            chapter = parts[3]

            target_dir = self.__temp_dir.joinpath(lang, resource, book, chapter, 'chapter')
            remote_dir = self.__ftp_dir.joinpath(lang, resource, book, chapter, "CONTENTS")
            verses_dir = target_dir.joinpath("verses")
            verses_dir.mkdir(parents=True, exist_ok=True)
            target_file = target_dir.joinpath(src_file.name)
            # Create new or update hash file of the chapter
            write_hash(src_file)

            # Copy source file to temp dir
            logging.debug(f'Copying file {src_file} to {target_file}')
            target_file.write_bytes(src_file.read_bytes())

            # Split chapter files into verses
            logging.debug(f'Splitting chapter {target_file} into {verses_dir}')
            split_chapter(target_file, verses_dir, self.verbose)

            if check_dir_empty(verses_dir):
                logging.warning(f'Could not split chapter file {target_file}. Verses dir is {verses_dir}. Make sure file is importable.')
                return

            target_verse_dir = remote_dir.joinpath("wav", "verse")

            is_new = check_dir_empty(target_verse_dir)
            is_changed = has_new_files(verses_dir, target_verse_dir)

            should_clean = is_new or is_changed

            # If we have a new or updated chapter WAV file
            # delete all the chapter related resources:
            # split verses, converted files, TR files and book files
            if should_clean:
                self.clean_derivatives(lang, resource, book, chapter)

            # Copy original verse files
            logging.debug(
                f'Copying original verse files from {verses_dir} into {remote_dir}'
            )
            t_dir = copy_dir(verses_dir, remote_dir)
            if should_clean and t_dir is not None:
                self.resources_created.append(str(rel_path(t_dir, self.__ftp_dir)))

            # Convert chapter to mp3
            self.convert_wav_to_mp3(target_file, remote_dir, 'chapter', 'hi')
            self.convert_wav_to_mp3(target_file, remote_dir, 'chapter', 'low')

            # Convert verses to mp3
            for f in verses_dir.iterdir():
                if f.is_dir():
                    continue

                self.convert_wav_to_mp3(f, remote_dir, 'verse', 'hi')
                self.convert_wav_to_mp3(f, remote_dir, 'verse', 'low')
        except Exception as e:
            logging.warning(f"exception in chapter worker: {e.with_traceback()}")
            
        # Process chapter files only
       

    def clean_derivatives(self, lang, resource, book, chapter):
        logging.debug(f'Chapter file has been changed, cleaning related files...')

        # Delete related chapter files
        self.delete_chapter_files(lang, resource, book, chapter)

        # Delete related verse files
        self.delete_verse_files(lang, resource, book, chapter)

        # Delete related book TR files
        self.delete_tr_book_files(lang, resource, book)

        # Delete related chapter TR files
        self.delete_tr_chapter_files(lang, resource, book, chapter)

        # Delete related book audio files
        self.delete_book_audio_files(lang, resource, book)

    def convert_wav_to_mp3(self, input_file: Path, remote_dir: Path, grouping: str, quality: str):
        if input_file.suffix != '.wav':
            pass

        bitrate = BITRATE_HIGH if quality == 'hi' else BITRATE_LOW

        # Check if file exists remotely
        mp3_exists = check_file_exists(input_file, remote_dir, 'mp3', grouping, quality)

        if not mp3_exists:
            logging.debug(f'Converting file: {input_file}')
            convert_to_mp3(input_file, bitrate, False, self.verbose)

            # Copy converted files
            mp3_file = input_file.with_suffix('.mp3')
            if mp3_file.exists():
                logging.debug(
                    f'Copying {mp3_file} to {remote_dir}'
                )
                m_file = copy_file(mp3_file, remote_dir, grouping, quality)
                self.resources_created.append(str(rel_path(m_file, self.__ftp_dir)))

            cue_file = input_file.with_suffix('.cue')
            if cue_file.exists():
                logging.debug(
                    f'Copying {cue_file} to {remote_dir}'
                )
                c_file = copy_file(cue_file, remote_dir, grouping)
                self.resources_created.append(str(rel_path(c_file, self.__ftp_dir)))
        else:
            logging.debug('File exists. Skipping...')

    def delete_tr_book_files(self, lang, resource, book):
        """ Delete tr files of the specified book """

        remote_book_dir = self.__ftp_dir.joinpath(lang, resource, book, "CONTENTS")
        book_name_tr = f'{lang}_{resource}_{book}.tr'

        wav_book_tr = remote_book_dir.joinpath("tr", "wav", "verse", book_name_tr)
        if wav_book_tr.exists():
            wav_book_tr.unlink()
            self.resources_deleted.append(str(rel_path(wav_book_tr, self.__ftp_dir)))

        mp3_hi_book_tr = remote_book_dir.joinpath("tr", "mp3", "hi", "verse", book_name_tr)
        if mp3_hi_book_tr.exists():
            mp3_hi_book_tr.unlink()
            self.resources_deleted.append(str(rel_path(mp3_hi_book_tr, self.__ftp_dir)))

        mp3_low_book_tr = remote_book_dir.joinpath("tr", "mp3", "low", "verse", book_name_tr)
        if mp3_low_book_tr.exists():
            mp3_low_book_tr.unlink()
            self.resources_deleted.append(str(rel_path(mp3_low_book_tr, self.__ftp_dir)))

    def delete_tr_chapter_files(self, lang, resource, book, chapter):
        """ Delete tr files of the specified chapter """

        remote_chapter_dir = self.__ftp_dir.joinpath(lang, resource, book, chapter, "CONTENTS")
        chapter_name_tr = f'{lang}_{resource}_{book}_c{chapter}.tr'

        wav_chapter_tr = remote_chapter_dir.joinpath("tr", "wav", "verse", chapter_name_tr)
        if wav_chapter_tr.exists():
            wav_chapter_tr.unlink()
            self.resources_deleted.append(str(rel_path(wav_chapter_tr, self.__ftp_dir)))

        mp3_hi_chapter_tr = remote_chapter_dir.joinpath("tr", "mp3", "hi", "verse", chapter_name_tr)
        if mp3_hi_chapter_tr.exists():
            mp3_hi_chapter_tr.unlink()
            self.resources_deleted.append(str(rel_path(mp3_hi_chapter_tr, self.__ftp_dir)))

        mp3_low_chapter_tr = remote_chapter_dir.joinpath("tr", "mp3", "low", "verse", chapter_name_tr)
        if mp3_low_chapter_tr.exists():
            mp3_low_chapter_tr.unlink()
            self.resources_deleted.append(str(rel_path(mp3_low_chapter_tr, self.__ftp_dir)))

    def delete_book_audio_files(self, lang, resource, book):
        """ Delete audio files of the specified book """

        remote_book_dir = self.__ftp_dir.joinpath(lang, resource, book, "CONTENTS")
        book_name = f'{lang}_{resource}_{book}'

        wav_book = remote_book_dir.joinpath("wav", "book", f'{book_name}.wav')
        if wav_book.exists():
            wav_book.unlink()
            self.resources_deleted.append(str(rel_path(wav_book, self.__ftp_dir)))

        mp3_hi_book = remote_book_dir.joinpath("mp3", "hi", "book", f'{book_name}.mp3')
        if mp3_hi_book.exists():
            mp3_hi_book.unlink()
            self.resources_deleted.append(str(rel_path(mp3_hi_book, self.__ftp_dir)))

        mp3_low_book = remote_book_dir.joinpath("mp3", "low", "book", f'{book_name}.mp3')
        if mp3_low_book.exists():
            mp3_low_book.unlink()
            self.resources_deleted.append(str(rel_path(mp3_low_book, self.__ftp_dir)))

    def delete_chapter_files(self, lang, resource, book, chapter):
        """ Delete chapter related files (mp3 and cue) """

        remote_chapter_dir = self.__ftp_dir.joinpath(lang, resource, book, chapter, "CONTENTS")

        mp3_hi_chapter_dir = remote_chapter_dir.joinpath("mp3", "hi", "chapter")
        if mp3_hi_chapter_dir.exists():
            rm_tree(mp3_hi_chapter_dir)
            self.resources_deleted.append(str(rel_path(mp3_hi_chapter_dir, self.__ftp_dir)))

        mp3_low_chapter_dir = remote_chapter_dir.joinpath("mp3", "low", "chapter")
        if mp3_low_chapter_dir.exists():
            rm_tree(mp3_low_chapter_dir)
            self.resources_deleted.append(str(rel_path(mp3_low_chapter_dir, self.__ftp_dir)))

        cue_chapter_dir = remote_chapter_dir.joinpath("cue", "chapter")
        if cue_chapter_dir.exists():
            rm_tree(cue_chapter_dir)
            self.resources_deleted.append(str(rel_path(cue_chapter_dir, self.__ftp_dir)))

    def delete_verse_files(self, lang, resource, book, chapter):
        """ Delete verse related files (wav, mp3, cue) """

        remote_chapter_dir = self.__ftp_dir.joinpath(lang, resource, book, chapter, "CONTENTS")

        wav_verse_files = remote_chapter_dir.joinpath("wav", "verse")
        if wav_verse_files.exists():
            rm_tree(wav_verse_files)
            self.resources_deleted.append(str(rel_path(wav_verse_files, self.__ftp_dir)))

        mp3_hi_verse_dir = remote_chapter_dir.joinpath("mp3", "hi", "verse")
        if mp3_hi_verse_dir.exists():
            rm_tree(mp3_hi_verse_dir)
            self.resources_deleted.append(str(rel_path(mp3_hi_verse_dir, self.__ftp_dir)))

        mp3_low_verse_dir = remote_chapter_dir.joinpath("mp3", "low", "verse")
        if mp3_low_verse_dir.exists():
            rm_tree(mp3_low_verse_dir)
            self.resources_deleted.append(str(rel_path(mp3_low_verse_dir, self.__ftp_dir)))

        cue_verse_dir = remote_chapter_dir.joinpath("cue", "verse")
        if cue_verse_dir.exists():
            rm_tree(cue_verse_dir)
            self.resources_deleted.append(str(rel_path(cue_verse_dir, self.__ftp_dir)))

    def get_report(self) -> Dict[str, list]:
        report = {
            "resources_created": self.resources_created,
            "resources_deleted": self.resources_deleted
        }
        return report

    def clear_report(self):
        self.resources_created.clear()
        self.resources_deleted.clear()

    @staticmethod
    def check_file_changed(file: Path) -> bool:
        changed = True
        hash_file = file.parent.joinpath(".hash")

        if hash_file.exists():
            with hash_file.open() as f:
                try:
                    hash_val = int(f.read())
                    if hash_val == read_hash(file):
                        changed = False
                except:
                    pass

        return changed

