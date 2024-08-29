import json
import logging
import re
import traceback
from pydub import AudioSegment
from pathlib import Path
from typing import List, Dict, Tuple

from file_utils import init_temp_dir, rm_tree, copy_file, rel_path
from constants import *
from concurrent.futures import ThreadPoolExecutor
from time import time
from functools import partial


class BookWorker:

    def __init__(self, input_dir: Path, verbose=False):
        self.__ftp_dir = input_dir
        self.__temp_dir = None
        self.verbose = verbose

        self.__book_verse_files = []
        self.__verse_regex = r"_c[\d]+_v[\d]+(?:_t[\d]+)?\..*$"

        self.resources_created = []
        self.resources_deleted = []
        # debugging, so just use one max worker
        self.thread_executor = ThreadPoolExecutor()

    def execute(self, all_files: set[Path]):
        """Execute worker"""

        logging.info("Book worker started!")
        start_time = time()
        self.thread_executor = ThreadPoolExecutor()
        try:
            self.clear_report()
            self.clear_cache()
            self.__temp_dir = init_temp_dir("book_worker_")

            (existent_books, verse_files) = (
                self.find_existent_books_and_filter_to_verse_files(all_files)
            )

            # Can't do these in parallel cause it calls a list.remove based on what's in the existent TR, which could cause a race condition if if thinks something is in the list, but soemthing else takes it out first.
            for file in verse_files:
                self.populate_book_verse_files(existent_books, file)
            logging.info(
                f"book_worker_log: Num verse files passed filters: {len(self.__book_verse_files)}.  "
            )

            # Create book files
            book_tuples = self.group_book_files()
            logging.info(
                f"book_worker_log: Num book groups to create: {len(book_tuples)}.  "
            )
            self.thread_executor.map(self.create_book_file, book_tuples)
        except Exception as e:
            traceback.print_exc()
        finally:
            logging.debug(f"Deleting temporary directory {self.__temp_dir}")
            rm_tree(self.__temp_dir)
            end_time = time()
            self.thread_executor.shutdown(wait=True)
            all_files.difference_update(set(self.resources_deleted))
            all_files.update(set(self.resources_created))
            logging.info(
                f"book_worker: removed {len(self.resources_deleted)} files: and added {len(self.resources_created)} files"
            )
            logging.info(f"Book worker  finished in {end_time - start_time} seconds!")

    def find_existent_books_and_filter_to_verse_files(
        self, all_files: set[Path]
    ) -> Tuple[List[Path], List[Path]]:
        """Find book files that exist in the remote directory"""

        existent_books = []
        verse_files = []
        verse_media = ["wav", "mp3/hi", "mp3/low"]
        book_media = [("wav", "wav"), ("mp3/hi", "mp3"), ("mp3/low", "mp3")]
        for src_file in all_files:
            # get verse files
            for m in verse_media:
                if not re.search(self.__verse_regex, str(src_file)):
                    continue
                if src_file.suffix == ".tr" or src_file.name == ".hash":
                    continue
                if f"{m}/verse/" in str(src_file):
                    verse_files.append(src_file)
            # check if matches book;
            for m, f in book_media:
                if src_file.suffix == f".{f}" and f"{m}/book/" in str(src_file):
                    existent_books.append(src_file)

        return (existent_books, verse_files)

    def populate_book_verse_files(self, existent_books: List[Path], src_file: Path):
        logging.debug(f"Book Worker: Found verse file: {src_file}")

        self.__book_verse_files.append(src_file)

        # Extract necessary path parts
        root_parts = self.__ftp_dir.parts
        parts = src_file.parts[len(root_parts) :]

        lang = parts[0]
        resource = parts[1]
        book = parts[2]
        media = parts[5]
        quality = parts[6] if media == "mp3" else ""

        regex = (
            rf"{lang}\/{resource}\/{book}\/" rf"CONTENTS\/{media}(?:\/{quality})?\/book"
        )

        for book in existent_books:
            if not re.search(regex, str(book)):
                continue

            if src_file in self.__book_verse_files:
                logging.debug(
                    f"Verse file {src_file} is excluded: exists in BOOK: {book}"
                )
                self.__book_verse_files.remove(src_file)

    def group_book_files(self) -> List[Tuple[str, List[Path]]]:
        """Group files into book groups"""

        dic = {}
        root_parts = self.__ftp_dir.parts
        for f in self.__book_verse_files:
            parent = f.parent
            parts = parent.parts[len(root_parts) :]

            lang = parts[0]
            resource = parts[1]
            book = parts[2]
            media = parts[5]
            quality = parts[6] if media == "mp3" else ""
            grouping = parts[7] if media == "mp3" else parts[6]

            parts_dic = {
                "lang": lang,
                "resource": resource,
                "book": book,
                "media": media,
                "quality": quality,
                "grouping": grouping,
            }

            key = json.dumps(parts_dic)

            if key not in dic:
                dic[key] = []
            dic[key].append(f)
        return list(dic.items())

    def create_book_file(self, info: Tuple[str, List[Path]]):
        """Create book file and copy it to the remote directory"""
        (dic, files) = info
        parts = json.loads(dic)
        lang = parts["lang"]
        resource = parts["resource"]
        book = parts["book"]
        media = parts["media"]
        quality = parts["quality"]
        remote_dir = self.__ftp_dir.joinpath(lang, resource, book, "CONTENTS")
        files.sort()
        # Create book file
        book_name = f"{lang}_{resource}_{book}.{media}"
        book = self.__temp_dir.joinpath(media, quality, book_name)
        book.parent.mkdir(parents=True, exist_ok=True)
        # Copy book file to remote dir
        self.merge_audio(book, files, media, quality)

        t_file = copy_file(book, remote_dir, "book", quality, media)
        self.resources_created.append(str(rel_path(t_file, self.__ftp_dir)))
        book.unlink()

    @staticmethod
    def merge_audio(target: Path, files: List[Path], media: str, quality: str):
        final_segment = AudioSegment.empty()
        for file in files:
            file_segment = AudioSegment.from_file(file, media)
            final_segment += file_segment

        # Bitrate values can be 64k, 128k, 320k, etc...
        bitrate = f"{BITRATE_HIGH}k" if quality == "hi" else f"{BITRATE_LOW}k"
        final_segment.export(target, media, bitrate=bitrate)

    def get_report(self) -> Dict[str, list]:
        report = {
            "resources_created": self.resources_created,
            "resources_deleted": self.resources_deleted,
        }
        return report

    def clear_report(self):
        self.resources_created.clear()
        self.resources_deleted.clear()

    def clear_cache(self):
        self.__book_verse_files = []
