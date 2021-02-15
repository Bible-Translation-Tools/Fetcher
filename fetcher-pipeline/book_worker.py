import json
import logging
import re
from pydub import AudioSegment
from pathlib import Path
from typing import List, Dict

from file_utils import init_temp_dir, rm_tree, copy_file, rel_path
from constants import *


class BookWorker:

    def __init__(self, input_dir: Path, verbose=False):
        self.__ftp_dir = input_dir
        self.__temp_dir = None
        self.verbose = verbose

        self.__book_verse_files = []
        self.__verse_regex = r'_c[\d]+_v[\d]+(?:_t[\d]+)?\..*$'

        self.resources_created = []
        self.resources_deleted = []

    def execute(self):
        """ Execute worker """

        logging.debug("Book worker started!")

        try:
            self.clear_report()
            self.clear_cache()
            self.__temp_dir = init_temp_dir("book_worker_")

            existent_books = self.find_existent_books()

            media = ['wav', 'mp3/hi', 'mp3/low']
            for m in media:
                for src_file in self.__ftp_dir.rglob(f'{m}/verse/*.*'):
                    if src_file.suffix == '.tr':
                        continue

                    # Process verse files only
                    if not re.search(self.__verse_regex, str(src_file)):
                        continue

                    logging.debug(f'Found verse file: {src_file}')

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
                            fr'CONTENTS\/{media}(?:\/{quality})?\/book'

                    for book in existent_books:
                        if not re.search(regex, str(book)):
                            continue

                        if src_file in self.__book_verse_files:
                            logging.debug(f'Verse file {src_file} is excluded: exists in BOOK: {book}')
                            self.__book_verse_files.remove(src_file)

            # Create book files
            book_groups = self.group_book_files()
            for key in book_groups:
                try:
                    self.create_book_file(key, book_groups[key])
                except Exception as e:
                    logging.warning(str(e))
        except Exception as e:
            logging.warning(str(e))
        finally:
            logging.debug(f'Deleting temporary directory {self.__temp_dir}')
            rm_tree(self.__temp_dir)

            logging.debug("Book worker finished!")

    def find_existent_books(self) -> List[Path]:
        """ Find book files that exist in the remote directory """

        existent_books = []
        media = [('wav', 'wav'), ('mp3/hi', 'mp3'), ('mp3/low', 'mp3')]
        for m, f in media:
            for src_file in self.__ftp_dir.rglob(f'{m}/book/*.{f}'):
                logging.debug(f'Found existent BOOK file: {src_file}')
                existent_books.append(src_file)

        return existent_books

    def group_book_files(self) -> Dict[str, List[Path]]:
        """ Group files into book groups """

        dic = {}
        root_parts = self.__ftp_dir.parts
        for f in self.__book_verse_files:
            parent = f.parent
            parts = parent.parts[len(root_parts):]

            lang = parts[0]
            resource = parts[1]
            book = parts[2]
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
