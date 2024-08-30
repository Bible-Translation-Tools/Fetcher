import json
import logging
import re
from enum import Enum
from pathlib import Path
from typing import List, Tuple, Dict
import traceback

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

        self.__verse_regex = r"_c[\d]+_v[\d]+(?:_t[\d]+)?\..*$"
        self.__tr_regex = (
            r"^.*?(?:\/([\d]+))?\/CONTENTS\/tr\/(?:wav|mp3)(?:\/(?:hi|low))?\/verse"
        )

        self.verbose = verbose

        self.resources_created = []
        self.resources_deleted = []
        self.thread_executor = ThreadPoolExecutor()

    def execute(self, all_files: set[Path]):
        """Execute worker"""
        start_time = time()
        # debugging, so just use one max worker
        self.thread_executor = ThreadPoolExecutor()
        logging.info("TR worker started!")
        try:
            self.clear_report()
            self.clear_cache()
            self.__temp_dir = init_temp_dir("tr_worker_")

            (existent_tr, verse_files) = self.get_existent_tr_and_verses_to_process(
                all_files
            )
            # Can't do these in parallel cause it calls a list.remove based on what's in the existent TR, which could cause a race condition if if thinks something is in the list, but soemthing else takes it out first. This fn will decide the __book_tr_files and __chapter_tr_files needing processing
            for file in verse_files:
                self.set_tr_files_to_process(existent_tr, file)

            # Each of these calls the thread executor process trs
            self.create_chapter_trs()
            self.create_book_trs()

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
                f"tr_worker: removed {len(self.resources_deleted)} files: and added {len(self.resources_created)} files"
            )
            logging.info(f"TR worker  finished in {end_time - start_time} seconds!")

    def set_tr_files_to_process(
        self, existent_tr: List[Tuple[Group, Path]], src_file: Path
    ):
        try:
            logging.debug(f"TR Worker: Found verse file: {src_file}")
            self.__book_tr_files.append(src_file)
            self.__chapter_tr_files.append(src_file)

            # Extract necessary path parts
            root_parts = self.__ftp_dir.parts
            parts = src_file.parts[len(root_parts) :]

            lang = parts[0]
            resource = parts[1]
            book = parts[2]
            chapter = parts[3]
            media = parts[5]
            quality = parts[6] if media == "mp3" else ""
            grouping = parts[7] if media == "mp3" else parts[6]

            regex = (
                rf"{lang}\/{resource}\/{book}(?:\/{chapter})?\/"
                rf"CONTENTS\/tr\/{media}(?:\/{quality})?\/{grouping}"
            )

            # Take out existing
            for group, tr in existent_tr:
                if not re.search(regex, str(tr)):
                    continue

                if group == Group.BOOK and src_file in self.__book_tr_files:
                    logging.debug(
                        f"Verse file {src_file} is excluded: exists in BOOK TR: {tr}"
                    )
                    self.__book_tr_files.remove(src_file)
                elif group == Group.CHAPTER and src_file in self.__chapter_tr_files:
                    logging.debug(
                        f"Verse file {src_file} is excluded: exists in CHAPTER TR: {tr}"
                    )
                    self.__chapter_tr_files.remove(src_file)
        except Exception as e:
            logging.warning(f"exception in tr_worker: {e}")
            traceback.print_exc()

    def get_existent_tr_and_verses_to_process(
        self, all_files: set[Path]
    ) -> Tuple[List[Tuple[Group, Path]], List[Path]]:
        """Find tr files that exist in the remote directory"""
        existent_tr = []
        verse_files = []
        verse_media = ["wav", "mp3/hi", "mp3/low"]
        # matches_glob = all_files

        for src_file in all_files:
            # gather verse, book and chapter files;
            for m in verse_media:
                # check for verse regex and not .tr
                if self.do_add_verse(m, src_file):
                    verse_files.append(src_file)
                #  check for
                do_include_regex = rf"tr/{m}/verse/.*.tr"
                if not re.search(do_include_regex, str(src_file)):
                    continue
                match = re.match(self.__tr_regex, str(src_file))
                if match.group(1) is not None:
                    logging.debug(
                        f"TR Worker: Found existent CHAPTER TR file: {src_file}"
                    )
                    existent_tr.append((Group.CHAPTER, src_file))
                else:
                    logging.debug(f"TR Worker: Found existent BOOK TR file: {src_file}")
                    existent_tr.append((Group.BOOK, src_file))

        return (existent_tr, verse_files)

    def do_add_verse(self, media: str, src_file: Path):
        if (
            not src_file.suffix == ".tr"
            and not src_file.name == ".hash"
            and re.search(self.__verse_regex, str(src_file))
            and f"{media}/verse/" in str(src_file)
        ):
            return True

    def group_files(
        self, files: List[Path], group: Group
    ) -> List[Tuple[str, List[Path]]]:
        """Group files into Book groups and Chapter groups"""

        dic = {}
        root_parts = self.__ftp_dir.parts
        for f in files:
            parent = f.parent
            parts = parent.parts[len(root_parts) :]

            lang = parts[0]
            resource = parts[1]
            book = parts[2]
            chapter = parts[3]
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

            if group == Group.CHAPTER:
                parts_dic["chapter"] = chapter

            key = json.dumps(parts_dic)

            if key not in dic:
                dic[key] = []
            dic[key].append(f)
        dic_tuple = list(dic.items())
        return dic_tuple

    def create_chapter_trs(self):
        chapter_groups = self.group_files(self.__chapter_tr_files, Group.CHAPTER)
        logging.info(
            f"There are {len(chapter_groups)} groups of chapter tr files to create"
        )
        self.thread_executor.map(self.create_tr_file, chapter_groups)

    def create_book_trs(self):
        book_groups = self.group_files(self.__book_tr_files, Group.BOOK)
        logging.info(f"There are {len(book_groups)} groups of book tr files to create")
        self.thread_executor.map(self.create_tr_file, book_groups)

    def create_tr_file(self, info: Tuple[str, List[Path]]):
        """Create tr file and copy it to the remote directory"""
        # runs in another thread, so exceptions don't bubble.  Own exception handling here
        try:
            (dic, files) = info
            parts = json.loads(dic)
            lang = parts["lang"]
            resource = parts["resource"]
            book = parts["book"]
            chapter = parts["chapter"] if "chapter" in parts else None
            media = parts["media"]
            quality = parts["quality"]
            grouping = parts["grouping"]
            root_dir = self.__temp_dir.joinpath("root")
            target_dir = root_dir.joinpath(lang, resource, book)
            if chapter is not None:
                remote_dir = self.__ftp_dir.joinpath(
                    lang, resource, book, chapter, "CONTENTS"
                )
            else:
                remote_dir = self.__ftp_dir.joinpath(lang, resource, book, "CONTENTS")
            for file in files:
                target_chapter = chapter
                if target_chapter is None:
                    match = re.search(r"_c([0-9]+)_v[0-9]+", file.name)
                    if not match:
                        raise Exception("Could not define chapter from the file name.")
                    target_chapter = match.group(1)

                target_chapter_dir = target_dir.joinpath(
                    self.zero_pad_chapter(target_chapter, book)
                )
                target_chapter_dir.mkdir(parents=True, exist_ok=True)

                target_file = target_chapter_dir.joinpath(file.name)
                # Copy source file to temp dir
                logging.debug(f"tr_worker_log: Copying file {file} to {target_file}")
                target_file.write_bytes(file.read_bytes())

            # Create TR file
            logging.debug("Creating TR file")
            create_tr(root_dir, self.verbose)
            tr = self.__temp_dir.joinpath("root.tr")

            if chapter is not None:
                new_tr = Path(tr.parent, f"{lang}_{resource}_{book}_c{chapter}.tr")
            else:
                new_tr = Path(tr.parent, f"{lang}_{resource}_{book}.tr")

            tr.rename(new_tr)

            # Copy tr file to remote dir
            t_file = copy_file(new_tr, remote_dir, grouping, quality, media)
            self.resources_created.append(str(rel_path(t_file, self.__ftp_dir)))
            # check: other worker threadsd might be depending on the this same shared tmep dir of /root/etc;... Just hoist this to be cleaned up later.  The new tr is just this one file though that this worker made, so I think ok to unlink.
            # todo: verify I can comment this out, and then it should just get cleaned up with the call to rm_tree(self.__temp_dir) in the finally block
            # rm_tree(root_dir)
            new_tr.unlink()
        except Exception as e:
            traceback.print_exc()

    @staticmethod
    def zero_pad_chapter(chapter: str, book: str) -> str:
        """Add leading zeros to the chapter depending on the book"""

        if book == "psa":
            return chapter.zfill(3)
        else:
            return chapter.zfill(2)

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
        self.__book_tr_files = []
        self.__chapter_tr_files = []
