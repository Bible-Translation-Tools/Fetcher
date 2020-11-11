import argparse
import logging
import os
from argparse import Namespace
from datetime import datetime
from pathlib import Path
from time import sleep
from typing import Tuple, List

import sentry_sdk

from chapter_worker import ChapterWorker
from tr_worker import TrWorker
from verse_worker import VerseWorker


class App:

    def __init__(self, input_dir: Path, verbose=False, hour=0, minute=0):
        self.__ftp_dir = input_dir
        self.verbose = verbose
        self.hour = hour
        self.minute = minute

        self.sleep_timer = 60

    def start(self):
        """ Start app """

        chapter_worker = ChapterWorker(self.__ftp_dir, self.verbose)
        verse_worker = VerseWorker(self.__ftp_dir, self.verbose)
        tr_worker = TrWorker(self.__ftp_dir, self.verbose)

        while True:
            now = datetime.now()
            target_time = now.replace(hour=self.hour, minute=self.minute, second=0)
            seconds_since_target_time = (now - target_time).total_seconds()

            if 0 <= seconds_since_target_time < self.sleep_timer:
                chapter_worker.execute()
                verse_worker.execute()
                tr_worker.execute()

                report = self.get_report(
                    (
                        chapter_worker.get_report(),
                        verse_worker.get_report(),
                        tr_worker.get_report()
                    )
                )
                if report is not None:
                    logging.error("Fetcher pipeline worker", extra=report)

            sleep(self.sleep_timer)

    @staticmethod
    def get_report(reports):
        """ Generate workers report """

        report = {
            "resources_created": [],
            "resources_deleted": [],
        }
        for r in reports:
            report["resources_created"] += r["resources_created"]
            report["resources_deleted"] += r["resources_deleted"]

        if (len(report["resources_created"]) > 0 or
                len(report["resources_deleted"]) > 0):
            return report

        return None


def get_arguments() -> Tuple[Namespace, List[str]]:
    """ Parse command line arguments """

    parser = argparse.ArgumentParser(description='Split and convert chapter files to mp3')
    parser.add_argument('-i', '--input-dir', type=lambda p: Path(p).absolute(), help='Input directory')
    parser.add_argument("-t", "--trace", action="store_true", help="Enable tracing output")
    parser.add_argument("-v", "--verbose", action="store_true", help="Enable logs from subprocess")
    parser.add_argument("-hr", "--hour", type=int, default=0, help="Hour, when to execute workers")
    parser.add_argument("-mn", "--minute", type=int, default=0, help="Minute, when to execute workers")

    return parser.parse_known_args()


def main():
    """ Launch application """

    args, unknown = get_arguments()

    if args.trace:
        log_level = logging.DEBUG
    else:
        log_level = logging.WARNING

    logging.basicConfig(format='%(asctime)s - %(levelname)s: %(message)s', level=log_level)

    sentry_sdk.init(
        os.getenv("SENTRY_DSN"),
        traces_sample_rate=0.0
    )

    app = App(args.input_dir, args.verbose, args.hour, args.minute)
    app.start()


if __name__ == "__main__":
    main()
