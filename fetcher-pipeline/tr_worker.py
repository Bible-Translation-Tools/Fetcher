import json
import logging
import re
from enum import Enum
from pathlib import Path
import threading
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
            logging.info(
                f"There are {len(existent_tr)} existent TR files, and {len(verse_files)} verse files to be combined into TR"
            )
            time_for_bytes = time()
            bytes_list = self.thread_executor.map(self.get_verse_bytes, verse_files)
            file_bytes_map: Dict[Path, bytes] = {}
            for file_path, file_bytes in zip(verse_files, bytes_list):
                file_bytes_map[file_path] = file_bytes
            logging.info(f"Elapsed time to get verse bytes: {time() - time_for_bytes}")
            book_trs = self.group_files(self.__book_tr_files, Group.BOOK)
            chapter_trs = self.group_files(self.__chapter_tr_files, Group.CHAPTER)
            logging.info(
                f"Processing {len(book_trs)} book trs and {len(chapter_trs)} chapter trs"
            )
            partial_fn = partial(self.create_tr_file, file_bytes_map)
            create_tr_time = time()
            self.thread_executor.map(partial_fn, book_trs)
            logging.info(f"Elapsed time to create book trs: {time() - create_tr_time}")
            self.thread_executor.map(partial_fn, chapter_trs)
            logging.info(
                f"Elapsed time to create chapter trs: {time() - create_tr_time}"
            )

        except Exception as e:
            traceback.print_exc()

        finally:
            logging.debug(f"Deleting temporary directory {self.__temp_dir}")
            # wait for all tasks to finish before removing anything
            self.thread_executor.shutdown(wait=True)
            rm_tree(self.__temp_dir)
            end_time = time()
            all_files.difference_update(set({Path(p) for p in self.resources_deleted}))
            all_files.update(set(Path(p) for p in self.resources_created))
            logging.info(
                f"tr_worker: removed {len(self.resources_deleted)} files: and added {len(self.resources_created)} files"
            )
            logging.info(f"TR worker  finished in {end_time - start_time} seconds!")

    def get_existent_tr_and_verses_to_process(
        self, all_files: set[Path]
    ) -> Tuple[List[Tuple[Group, Path]], List[Path]]:
        """Find tr files that exist in the remote directory"""
        existent_tr = {str(path.parent) for path in all_files if path.suffix == ".tr"}
        already_filtered = []
        verse_media = ["wav", "mp3/hi", "mp3/low"]
        # matches_glob = all_files
        for src_file in all_files:
            # gather verse, book and chapter files;
            for m in verse_media:
                # check for verse regex and not .tr
                if self.do_add_verse(m, src_file):
                    # continue

                    # verse_files.append(src_file)
                    root_parts = self.__ftp_dir.parts
                    parts = src_file.parts[len(root_parts) :]
                    parent = str(src_file.parent).replace(m, f"tr/{m}")

                    if not parent in existent_tr:
                        self.__book_tr_files.append(src_file)
                        self.__chapter_tr_files.append(src_file)
                        already_filtered.append(src_file)
                #  check for
                # do_include_regex = rf"tr/{m}/verse/.*.tr"
                # if not re.search(do_include_regex, str(src_file)):
                #     continue
                # match = re.match(self.__tr_regex, str(src_file))
                # root_parts = self.__ftp_dir.parts
                # parts = src_file.parts[len(root_parts) :]
                # if all_files contains a tr file (book) or chapter
                # if match.group(1) is not None:
                #     logging.debug(
                #         f"TR Worker: Found existent CHAPTER TR file: {src_file}"
                #     )
                #     existent_tr.append((Group.CHAPTER, src_file))
                # else:
                #     logging.debug(f"TR Worker: Found existent BOOK TR file: {src_file}")
                #     existent_tr.append((Group.BOOK, src_file))

        return (existent_tr, already_filtered)

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

    def get_verse_bytes(self, src_file: Path):
        with open(src_file, "rb") as f:
            return f.read()

    def create_tr_file(
        self, file_bytes_map: Dict[Path, bytes], info: Tuple[str, List[Path]]
    ):
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
            thread_temp_dir = self.__temp_dir.joinpath(
                f"thread-{threading.get_ident()}"
            )
            thread_temp_dir.mkdir(parents=True, exist_ok=True)
            root_dir = thread_temp_dir.joinpath("root")
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
                matching_bytes = file_bytes_map[file]
                target_file.write_bytes(matching_bytes)

            # Create TR file
            logging.debug("Creating TR file")
            create_tr(root_dir, self.verbose)
            tr = thread_temp_dir.joinpath("root.tr")

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
            logging.warning(f"exception: {e}")
            logging.warning(f"file is {file}")
            logging.warning(f"target file was {target_file}")
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
