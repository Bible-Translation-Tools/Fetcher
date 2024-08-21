import json
import logging
import re
from enum import Enum
from pathlib import Path
from typing import List, Tuple, Dict

from file_utils import init_temp_dir, rm_tree, copy_file, rel_path
from process_tools import create_tr
from concurrent.futures import ThreadPoolExecutor
from time import time
from functools import partial


class Group(Enum):
    BOOK = 1
    CHAPTER = 2


class TrWorker:

    def __init__(self, input_dir: Path, verbose=False):
        self.__ftp_dir = input_dir
        self.__temp_dir = None

        self.__book_tr_files = []
        self.__chapter_tr_files = []

        self.__verse_regex = r'_c[\d]+_v[\d]+(?:_t[\d]+)?\..*$'
        self.__tr_regex = r'^.*?(?:\/([\d]+))?\/CONTENTS\/tr\/(?:wav|mp3)(?:\/(?:hi|low))?\/verse'

        self.verbose = verbose

        self.resources_created = []
        self.resources_deleted = []
        self.thread_executor = ThreadPoolExecutor()
        

    def execute(self, all_files: set[Path]):
        """ Execute worker """
        start_time = time()
        logging.info("TR worker started!")
        try:
            self.clear_report()
            self.clear_cache()
            self.__temp_dir = init_temp_dir("tr_worker_")
            
            (existent_tr, verse_files) = self.get_existent_tr_and_verses_to_process(all_files)
            # Partially apply the existent_tr argument so we can call fn sig of thread map of fn, iterable
            set_tr_files_partial = partial(self.set_tr_files_to_process, existent_tr)
            self.thread_executor.map(set_tr_files_partial, verse_files)
             # Each of these calls the thread executor process trs
            self.create_chapter_trs()
            self.create_book_trs()
            all_files.difference_update(set(self.resources_deleted))
            all_files.update(set(self.resources_created))
            
            
        finally:
            logging.debug(f'Deleting temporary directory {self.__temp_dir}')
            rm_tree(self.__temp_dir)
            end_time = time()
            logging.info(f"TR worker  finished in {end_time - start_time} seconds!")

    
    def set_tr_files_to_process(self, src_file: Path, existent_tr: List[Tuple[Group, Path]]):
        try:
            logging.debug(f'Found verse file: {src_file}')
            self.__book_tr_files.append(src_file)
            self.__chapter_tr_files.append(src_file)

            # Extract necessary path parts
            root_parts = self.__ftp_dir.parts
            parts = src_file.parts[len(root_parts):]

            lang = parts[0]
            resource = parts[1]
            book = parts[2]
            chapter = parts[3]
            media = parts[5]
            quality = parts[6] if media == 'mp3' else ''
            grouping = parts[7] if media == 'mp3' else parts[6]

            regex = fr'{lang}\/{resource}\/{book}(?:\/{chapter})?\/' \
                    fr'CONTENTS\/tr\/{media}(?:\/{quality})?\/{grouping}'

            # Take out existing
            for group, tr in existent_tr:
                if not re.search(regex, str(tr)):
                    continue

                if group == Group.BOOK and src_file in self.__book_tr_files:
                    logging.debug(f'Verse file {src_file} is excluded: exists in BOOK TR: {tr}')
                    self.__book_tr_files.remove(src_file)
                elif group == Group.CHAPTER and src_file in self.__chapter_tr_files:
                    logging.debug(f'Verse file {src_file} is excluded: exists in CHAPTER TR: {tr}')
                    self.__chapter_tr_files.remove(src_file)
        except Exception as e:
            logging.warning(f"exception in tr_worker: {e.with_traceback()}")
        
    def get_existent_tr_and_verses_to_process(self, all_files: set[Path]) -> Tuple[List[Tuple[Group, Path]], List[Path]]:
        """ Find tr files that exist in the remote directory """
        existent_tr = []
        verse_files = []
        verse_media = ['wav', 'mp3/hi', 'mp3/low']
        for src_file in all_files:
            # gather verse files: 
            for m in verse_media:
                if src_file.suffix == '.tr':
                    continue
                if not re.search(self.__verse_regex, str(src_file)):
                    continue
                if f'{m}/verse/' in str(src_file):
                    verse_files.append(src_file)
            # Gather existing tr files
            match = re.match(self.__tr_regex, str(src_file))
            if match.group(1) is not None:
                logging.debug(f'Found existent CHAPTER TR file: {src_file}')
                existent_tr.append((Group.CHAPTER, src_file))
            else:
                logging.debug(f'Found existent BOOK TR file: {src_file}')
                existent_tr.append((Group.BOOK, src_file))

        return (existent_tr, verse_files)

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

    def create_chapter_trs(self):
        chapter_groups = self.group_files(self.__chapter_tr_files, Group.CHAPTER)
        for key in chapter_groups: 
            try:
                partial_create_tr = partial(self.create_tr_file, key)
                self.thread_executor.map(
                    partial_create_tr, 
                    chapter_groups[key]
                )
            except Exception as e:
                logging.warning(f"exception in tr_worker create_chapter_trs: {e.with_traceback()}")

    def create_book_trs(self):
        book_groups = self.group_files(self.__book_tr_files, Group.BOOK)
         # Spawn thread for each book; 
        for key in book_groups:
            partial_create_tr = partial(self.create_tr_file, key)
            try:
                self.thread_executor.map(
                    partial_create_tr,
                    book_groups[key]
                )
            except Exception as e:
                logging.warning(f"exception in tr_worker create_book_trs: {e.with_traceback()}")

    def create_tr_file(self, dic: str, files: List[Path]):
        """ Create tr file and copy it to the remote directory"""

        parts = json.loads(dic)

        lang = parts['lang']
        resource = parts['resource']
        book = parts['book']
        chapter = parts['chapter'] if 'chapter' in parts else None
        media = parts['media']
        quality = parts['quality']
        grouping = parts['grouping']

        root_dir = self.__temp_dir.joinpath('root')
        target_dir = root_dir.joinpath(lang, resource, book)

        if chapter is not None:
            remote_dir = self.__ftp_dir.joinpath(lang, resource, book, chapter, "CONTENTS")
        else:
            remote_dir = self.__ftp_dir.joinpath(lang, resource, book, "CONTENTS")

        for file in files:
            target_chapter = chapter
            if target_chapter is None:
                match = re.search(r'_c([0-9]+)_v[0-9]+', file.name)
                if not match:
                    raise Exception('Could not define chapter from the file name.')
                target_chapter = match.group(1)

            target_chapter_dir = target_dir.joinpath(self.zero_pad_chapter(target_chapter, book))
            target_chapter_dir.mkdir(parents=True, exist_ok=True)

            target_file = target_chapter_dir.joinpath(file.name)

            # Copy source file to temp dir
            logging.debug(f'Copying file {file} to {target_file}')
            target_file.write_bytes(file.read_bytes())

        # Create TR file
        logging.debug('Creating TR file')
        create_tr(root_dir, self.verbose)
        tr = self.__temp_dir.joinpath('root.tr')

        if chapter is not None:
            new_tr = Path(tr.parent, f'{lang}_{resource}_{book}_c{chapter}.tr')
        else:
            new_tr = Path(tr.parent, f'{lang}_{resource}_{book}.tr')

        tr.rename(new_tr)

        # Copy tr file to remote dir
        logging.debug(
            f'Copying {new_tr} to {remote_dir}'
        )
        t_file = copy_file(new_tr, remote_dir, grouping, quality, media)
        self.resources_created.append(str(rel_path(t_file, self.__ftp_dir)))

        rm_tree(root_dir)
        new_tr.unlink()

    @staticmethod
    def zero_pad_chapter(chapter: str, book: str) -> str:
        """ Add leading zeros to the chapter depending on the book """

        if book == 'psa':
            return chapter.zfill(3)
        else:
            return chapter.zfill(2)

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
        self.__book_tr_files = []
        self.__chapter_tr_files = []
