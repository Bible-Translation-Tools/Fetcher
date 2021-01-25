import json
import logging
import re
from enum import Enum

from pydub import AudioSegment
from pathlib import Path
from typing import List, Dict, Tuple

from file_utils import init_temp_dir, rm_tree, copy_file, rel_path
from constants import *


class Group(Enum):
    BOOK = 1
    CHAPTER = 2


class CompileWorker:

    def __init__(self, input_dir: Path, verbose=False):
        self.__ftp_dir = input_dir
        self.__temp_dir = None
        self.verbose = verbose

        self.__chapter_verse_files = []
        self.__book_verse_files = []
        self.__verse_regex = r'_c[\d]+_v[\d]+(?:-[\d]+)?(?:_t[\d]+)?\..*$'

        self.resources_created = []
        self.resources_deleted = []

    def execute(self):
        """ Execute worker """

        logging.debug("-------------------------------")
        logging.debug("----- Compile worker started! ----")
        logging.debug("-------------------------------")

        try:
            self.clear_report()
            self.clear_cache()
            self.__temp_dir = init_temp_dir("book_worker_")

            existent_compilations = self.find_existent_compilations()

            media = ['wav', 'mp3/hi', 'mp3/low']
            for m in media:
                for src_file in self.__ftp_dir.rglob(f'{m}/chunk/*.*'):
                    if src_file.suffix == '.tr':
                        continue

                    # Process verse files only
                    if not re.search(self.__verse_regex, str(src_file)):
                        continue

                    logging.debug(f'Found verse file: {src_file}')

                    self.__chapter_verse_files.append(src_file)
                    self.__book_verse_files.append(src_file)

                    # Extract necessary path parts
                    root_parts = self.__ftp_dir.parts
                    parts = src_file.parts[len(root_parts):]

                    lang = parts[0]
                    resource = parts[1]
                    book = parts[2]
                    media = parts[5]
                    quality = parts[6] if media == 'mp3' else ''

                    regex = fr'{lang}\/{resource}\/{book}\/' \
                            fr'CONTENTS\/{media}(?:\/{quality})?\/'

                    for group, compilation in existent_compilations:
                        if not re.search(regex + r'book', str(compilation)) \
                                and not re.search(regex + r'chapter', str(compilation)):
                            continue

                        if group == Group.BOOK and src_file in self.__book_verse_files:
                            logging.debug(f'Verse file {src_file} is excluded: exists in BOOK TR: {compilation}')
                            self.__book_verse_files.remove(src_file)
                        elif group == Group.CHAPTER and src_file in self.__chapter_verse_files:
                            logging.debug(f'Verse file {src_file} is excluded: exists in CHAPTER TR: {compilation}')
                            self.__chapter_verse_files.remove(src_file)

            # Create chapter compilation
            chapter_groups = self.group_files(self.__chapter_verse_files, Group.CHAPTER)
            for key in chapter_groups:
                self.create_compilation_file(key, chapter_groups[key])

            # Create book compilation
            book_groups = self.group_files(self.__book_verse_files, Group.BOOK)
            for key in book_groups:
                self.create_compilation_file(key, book_groups[key])
        except Exception as e:
            logging.warning(str(e))
        finally:
            logging.debug(f'Deleting temporary directory {self.__temp_dir}')
            rm_tree(self.__temp_dir)

            logging.debug('Compile worker finished!')

    def find_existent_compilations(self) -> List[Tuple[Group, Path]]:
        """ Find compiled chapter and book wav files that exist in the remote directory """

        existent_compilations = []
        media = [('wav', 'wav'), ('mp3/hi', 'mp3'), ('mp3/low', 'mp3')]
        for m, f in media:
            for src_file in self.__ftp_dir.rglob(f'{m}/chapter/*.{f}'):
                logging.debug(f'Found existent CHAPTER file: {src_file}')
                existent_compilations.append((Group.CHAPTER, src_file))

            for src_file in self.__ftp_dir.rglob(f'{m}/book/*.{f}'):
                logging.debug(f'Found existent BOOK file: {src_file}')
                existent_compilations.append((Group.BOOK, src_file))

        return existent_compilations

    def group_files(self, files: List[Path], group: Group) -> Dict[str, List[Path]]:
        """ Group files into Book groups and Chapter groups """

        dic = {}
        root_parts = self.__ftp_dir.parts
        for f in files:
            parent = f.parent
            parts = parent.parts[len(root_parts):]

            lang = parts[0]
            resource = parts[1]
            book = parts[2]
            chapter = parts[3]
            media = parts[5]
            quality = parts[6] if media == 'mp3' else ''
            grouping = parts[7] if media == 'mp3' else parts[6]

            parts_dic = {
                'lang': lang,
                'resource': resource,
                'book': book,
                'media': media,
                'quality': quality,
                'grouping': grouping
            }

            if group == Group.CHAPTER:
                parts_dic['chapter'] = chapter

            key = json.dumps(parts_dic)

            if key not in dic:
                dic[key] = []
            dic[key].append(f)

        return dic

    def create_book_file(self, dic: str, files: List[Path]):
        """ Create book file and copy it to the remote directory"""

        parts = json.loads(dic)

        lang = parts['lang']
        resource = parts['resource']
        book = parts['book']
        media = parts['media']
        quality = parts['quality']

        remote_dir = self.__ftp_dir.joinpath(lang, resource, book, "CONTENTS")

        files.sort()

        # Create book file
        logging.debug('Creating book file')

        book_name = f'{lang}_{resource}_{book}.{media}'
        book = self.__temp_dir.joinpath(media, quality, book_name)
        book.parent.mkdir(parents=True, exist_ok=True)

        self.merge_audio(book, files, media, quality)

        # Copy book file to remote dir
        logging.debug(f'Copying {book} to {remote_dir}')
        t_file = copy_file(book, remote_dir, 'book', quality, media)
        self.resources_created.append(str(rel_path(t_file, self.__ftp_dir)))

        book.unlink()

    def create_compilation_file(self, dic: str, files: List[Path]):
        """ Create book or chapter compilation file and copy it to the remote directory"""

        parts = json.loads(dic)

        lang = parts['lang']
        resource = parts['resource']
        book = parts['book']
        chapter = parts['chapter'] if 'chapter' in parts else None
        media = parts['media']
        quality = parts['quality']

        is_chapter = True if chapter is not None else False
        grouping = 'chapter' if is_chapter else 'book'

        if is_chapter:
            remote_dir = self.__ftp_dir.joinpath(lang, resource, book, chapter, "CONTENTS")
        else:
            remote_dir = self.__ftp_dir.joinpath(lang, resource, book, "CONTENTS")

        files.sort()

        # Create compilation file
        logging.debug(f'Creating {"CHAPTER" if is_chapter else "BOOK"} compilation file')

        compilation_name = f'{lang}_{resource}_{book}{"_c"+chapter if is_chapter else ""}.{media}'
        compilation = self.__temp_dir.joinpath(media, quality, compilation_name)
        compilation.parent.mkdir(parents=True, exist_ok=True)

        self.merge_audio(compilation, files, media, quality)

        # Copy book file to remote dir
        logging.debug(f'Copying {compilation} to {remote_dir}')
        t_file = copy_file(compilation, remote_dir, grouping, quality, media)
        self.resources_created.append(str(rel_path(t_file, self.__ftp_dir)))

        compilation.unlink()

    @staticmethod
    def merge_audio(target: Path, files: List[Path], media: str, quality: str):
        final_segment = AudioSegment.empty()
        for file in files:
            file_segment = AudioSegment.from_file(file, media)
            final_segment += file_segment

        # Bitrate values can be 64k, 128k, 320k, etc...
        bitrate = f'{BITRATE_HIGH}k' if quality == 'hi' else f'{BITRATE_LOW}k'
        final_segment.export(target, media, bitrate=bitrate)

    def get_report(self) -> Dict[str, list]:
        report = {
            "resources_created": self.resources_created,
            "resources_deleted": self.resources_deleted
        }
        return report

    def clear_report(self):
        self.resources_created.clear()
        self.resources_deleted.clear()

    def clear_cache(self):
        self.__book_verse_files = []
