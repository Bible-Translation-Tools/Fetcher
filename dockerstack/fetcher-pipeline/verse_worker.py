import logging
import re
from pathlib import Path
from typing import Dict

from file_utils import init_temp_dir, rm_tree, copy_file, check_file_exists, rel_path
from process_tools import fix_metadata, convert_to_mp3


class VerseWorker:

    def __init__(self, input_dir: Path, verbose=False):
        self.__ftp_dir = input_dir
        self.__temp_dir = None

        self.__verse_regex = r'_c[\d]+_v[\d]+(?:-[\d]+)?(?:_t[\d]+)?\..*$'

        self.verbose = verbose

        self.resources_created = []
        self.resources_deleted = []

    def execute(self):
        logging.debug("Verse worker started!")

        self.clear_report()
        self.__temp_dir = init_temp_dir()

        for src_file in self.__ftp_dir.rglob('*.wav'):
            # Process verse/chunk files only
            if not re.search(self.__verse_regex, str(src_file)):
                continue

            # Extract necessary path parts
            root_parts = self.__ftp_dir.parts
            parts = src_file.parts[len(root_parts):]

            lang = parts[0]
            resource = parts[1]
            book = parts[2]
            chapter = parts[3]
            media = parts[5]
            grouping = parts[7] if media == 'mp3' else parts[6]

            target_dir = self.__temp_dir.joinpath(lang, resource, book, chapter, grouping)
            remote_dir = self.__ftp_dir.joinpath(lang, resource, book, chapter, "CONTENTS")
            target_dir.mkdir(parents=True, exist_ok=True)
            target_file = target_dir.joinpath(src_file.name)

            logging.debug(f'Found verse file: {src_file}')

            mp3_exists = check_file_exists(src_file, remote_dir, 'mp3', grouping)
            cue_exists = check_file_exists(src_file, remote_dir, 'cue', grouping)

            if mp3_exists and cue_exists:
                logging.debug(f'Files exist. Skipping...')
                continue

            # Copy source file to temp dir
            logging.debug(f'Copying file {src_file} to {target_file}')
            target_file.write_bytes(src_file.read_bytes())

            # Try to fix wav metadata
            logging.debug(f'Fixing metadata: {target_file}')
            fix_metadata(target_file, self.verbose)

            # Convert verse into mp3
            self.convert_verse(target_file, remote_dir, grouping)

        logging.debug(f'Deleting temporary directory {self.__temp_dir}')
        rm_tree(self.__temp_dir)

        logging.debug('Verse worker finished!')

    def convert_verse(self, verse_file: Path, remote_dir: Path, grouping: str):
        """ Convert verse wav file and copy to remote directory """

        logging.debug(f'Converting verse: {verse_file}')
        convert_to_mp3(verse_file, self.verbose)

        # Copy converted verse file (mp3 and cue)
        mp3_file = verse_file.with_suffix('.mp3')
        logging.debug(
            f'Copying verse mp3 {mp3_file} into {remote_dir}'
        )
        if mp3_file.exists():
            m_file = copy_file(mp3_file, remote_dir, grouping)
            self.resources_created.append(str(rel_path(m_file, self.__ftp_dir)))

        cue_file = verse_file.with_suffix('.cue')
        logging.debug(
            f'Copying verse cue {cue_file} into {remote_dir}'
        )
        if cue_file.exists():
            c_file = copy_file(cue_file, remote_dir, grouping)
            self.resources_created.append(str(rel_path(c_file, self.__ftp_dir)))

    def get_report(self) -> Dict[str, list]:
        report = {
            "resources_created": self.resources_created,
            "resources_deleted": self.resources_deleted
        }
        return report

    def clear_report(self):
        self.resources_created.clear()
        self.resources_deleted.clear()
