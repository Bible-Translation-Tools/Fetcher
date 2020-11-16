import argparse
import time
import os
import glob
from pathlib import Path
from datetime import datetime
from time import sleep
from argparse import Namespace
from typing import Tuple, List


class DirectoryCleaner:

    def __init__(self, input_dir: Path, verbose=False, hour=0, minute=0):
        self.content_dir = input_dir
        self.verbose = verbose
        self.hour = hour
        self.minute = minute

        self.sleep_timer = 60
        self.max_allowed_file_age = 2 * 7 * 24 * 60 * 60  # number of seconds in two weeks

    def start(self):

        while True:
            now = datetime.now()
            target_time = now.replace(hour=self.hour, minute=self.minute, second=0)
            seconds_since_target_time = (now - target_time).total_seconds()

            if 0 <= seconds_since_target_time < self.sleep_timer:
                self.delete_stuff_in_dir()

            sleep(self.sleep_timer)

    def delete_stuff_in_dir(self):
        file_paths = glob.glob(self.content_dir + "/*.zip")

        for file_path in file_paths:
            time_created = time.ctime(os.path.getctime(file_path))
            time_created_date = datetime.strptime(time_created, '%a %b %d %X %Y')
            file_age = (datetime.now() - time_created_date).total_seconds()

            if file_age > self.max_allowed_file_age:
                os.remove(file_path)


def get_arguments() -> Tuple[Namespace, List[str]]:

    parser = argparse.ArgumentParser(description='')

    parser.add_argument('-i', '--input-dir', type=lambda p: Path(p).absolute(), help='Input Directory')
    parser.add_argument('-t', '--trace', action='store_true', help='Enable tracing output')
    parser.add_argument('-v', '--verbose', action='store_true', help='Enable logs from subprocess')
    parser.add_argument('-hr', '--hour', type=int, default=0, help='Hour, when to delete directories')
    parser.add_argument('-mn', '--minute', type=int, default=0, help='Minute, when to delete directories')

    return parser.parse_known_args()


def main():

    args, unknown = get_arguments()

    app = DirectoryCleaner(args.input_dir, args.verbose, args.hour, args.minute)
    app.delete_stuff_in_dir()


if __name__ == "__main__":
    main()

