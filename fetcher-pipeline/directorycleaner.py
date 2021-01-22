import argparse
import glob
import logging
import os
import shutil
import time
from argparse import Namespace
from datetime import datetime
from time import sleep
from typing import Tuple, List


class DirectoryCleaner:

    def __init__(self, hour=1, minute=0):
        self.content_dir = os.getenv("RC_TEMP_DIR")
        self.hour = hour
        self.minute = minute

        self.max_allowed_file_age = 2 * 7 * 24 * 60 * 60  # number of seconds in two weeks

    def start(self):
        wait_timer = (self.hour * 3600) + (self.minute * 60)

        if wait_timer == 0:
            logging.debug("Set timer to more than zero minutes")
            exit(0)

        while True:
            self.delete_temp_rc_content()
            sleep(wait_timer)

    def delete_temp_rc_content(self):
        file_paths = glob.glob("{}/*".format(self.content_dir))

        for file_path in file_paths:
            time_created = time.ctime(os.path.getctime(file_path))
            time_created_date = datetime.strptime(time_created, '%a %b %d %X %Y')
            file_age = (datetime.now() - time_created_date).total_seconds()

            if file_age > self.max_allowed_file_age:
                shutil.rmtree(file_path)
                logging.info("File deleted at {}".format(file_path))


def get_arguments() -> Tuple[Namespace, List[str]]:

    parser = argparse.ArgumentParser(description='')

    parser.add_argument("-t", "--trace", action="store_true", help="Enable tracing output")
    parser.add_argument('-hr', '--hour', type=int, default=0, help='Frequency when to delete directories in hours')
    parser.add_argument('-mn', '--minute', type=int, default=0, help='Frequency when to delete directories in minutes')

    return parser.parse_known_args()


def main():

    args, unknown = get_arguments()

    if args.trace:
        log_level = logging.DEBUG
    else:
        log_level = logging.WARNING

    logging.basicConfig(format='%(asctime)s - %(levelname)s: %(message)s', level=log_level)

    app = DirectoryCleaner(args.hour, args.minute)
    app.start()


if __name__ == "__main__":
    main()

