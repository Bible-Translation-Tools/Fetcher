import logging
import re
from pathlib import Path
import traceback
from typing import Dict

from file_utils import init_temp_dir, rm_tree, copy_file, check_file_exists, rel_path
from process_tools import convert_to_mp3
from constants import *
from time import time
from concurrent.futures import ThreadPoolExecutor


class VerseWorker:

    def __init__(self, input_dir: Path, verbose=False):
        self.__ftp_dir = input_dir
        self.__temp_dir = None

        self.__verse_regex = r"_c[\d]+_v[\d]+(?:-[\d]+)?(?:_t[\d]+)?\..*$"

        self.verbose = verbose

        self.resources_created = []
        self.resources_deleted = []
        self.thread_executor = ThreadPoolExecutor()

    def execute(self, all_files: set[Path]):
        """Execute worker"""
        start_time = time()
        try:
            logging.info("Verse worker started!")
            # reinvoke due to runs due to explicity shutdown call in finall clause
            self.thread_executor = ThreadPoolExecutor()
            self.clear_report()
            self.__temp_dir = init_temp_dir("verse_worker_")
            files_to_process = {path for path in all_files if path.suffix == ".wav"}
            verses_filtered = [
                path for path in files_to_process if self.include_file(path)
            ]
            logging.info(
                f"starting process_verse with {len(verses_filtered) } verses to process"
            )
            self.thread_executor.map(self.process_verse, verses_filtered)
        except Exception as e:
            traceback.print_exc()
        finally:
            self.thread_executor.shutdown(wait=True)
            all_files.difference_update(set(self.resources_deleted))
            all_files.update(set(self.resources_created))
            logging.info(
                f"verse_worker: removed {len(self.resources_deleted)} files: and added {len(self.resources_created)} files"
            )
            logging.debug(f"Deleting temporary directory {self.__temp_dir}")
            rm_tree(self.__temp_dir)
            end_time = time()
            logging.info(f"Verse worker finished in {end_time - start_time} seconds!")

    def include_file(self, file: Path) -> bool:
        return re.search(self.__verse_regex, str(file))

    def process_verse(self, src_file):
        # exception handling in here and not only at top since this runs in multiple threads
        try:
            logging.debug(f"Verse Worker: Found verse file: {src_file}")

            # Extract necessary path parts
            root_parts = self.__ftp_dir.parts
            parts = src_file.parts[len(root_parts) :]
            lang = parts[0]
            resource = parts[1]
            book = parts[2]
            chapter = parts[3]
            media = parts[5]
            grouping = parts[7] if media == "mp3" else parts[6]

            remote_dir = self.__ftp_dir.joinpath(
                lang, resource, book, chapter, "CONTENTS"
            )
            mp3_exists = check_file_exists(src_file, remote_dir, "mp3", grouping)
            cue_exists = check_file_exists(src_file, remote_dir, "cue", grouping)

            if mp3_exists and cue_exists:
                logging.debug(f"Files exist. Skipping...")
                return

            target_dir = self.__temp_dir.joinpath(
                lang, resource, book, chapter, grouping
            )
            target_dir.mkdir(parents=True, exist_ok=True)
            target_file = target_dir.joinpath(src_file.name)

            # Copy source file to temp dir
            logging.debug(f"Copying file {src_file} to {target_file}")
            target_file.write_bytes(src_file.read_bytes())

            # Convert verse into mp3
            self.convert_verse_wav(target_file, remote_dir, grouping, "hi")
            self.convert_verse_wav(target_file, remote_dir, grouping, "low")
        except Exception as e:
            logging.warning(f"exception in verse_worker {e}")
            traceback.print_exc()

    def convert_verse_wav(
        self, verse_file: Path, remote_dir: Path, grouping: str, quality: str
    ):
        """Convert verse wav file and copy to remote directory"""

        if verse_file.suffix != ".wav":
            pass

        bitrate = BITRATE_HIGH if quality == "hi" else BITRATE_LOW

        logging.debug(f"Converting verse: {verse_file}")
        convert_to_mp3(verse_file, bitrate, False, self.verbose)

        # Copy converted verse file (mp3 and cue)
        mp3_file = verse_file.with_suffix(".mp3")
        logging.debug(f"Copying verse mp3 {mp3_file} into {remote_dir}")
        if mp3_file.exists():
            m_file = copy_file(mp3_file, remote_dir, grouping, quality)
            self.resources_created.append(str(rel_path(m_file, self.__ftp_dir)))

        cue_file = verse_file.with_suffix(".cue")
        logging.debug(f"Copying verse cue {cue_file} into {remote_dir}")
        if cue_file.exists():
            c_file = copy_file(cue_file, remote_dir, grouping)
            self.resources_created.append(str(rel_path(c_file, self.__ftp_dir)))

    def get_report(self) -> Dict[str, list]:
        report = {
            "resources_created": self.resources_created,
            "resources_deleted": self.resources_deleted,
        }
        return report

    def clear_report(self):
        self.resources_created.clear()
        self.resources_deleted.clear()
