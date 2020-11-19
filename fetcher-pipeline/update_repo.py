import argparse
import datetime
import os
import logging
import requests
from time import sleep
from argparse import Namespace
from typing import Tuple, List

from src.process_tools import git_clone, pull_all_repos

GL_REPO_URLS = "gl_repo_urls.txt"


class RepositoryUpdater:

    def __init__(self, verbose, hour, minute):
        self.verbose = verbose
        self.hour = hour
        self.minute = minute
        self.repo_dir = os.getenv("ORATURE_REPO_DIR")
        self.sleep_timer = 60

    def repo_exists(self, name):
        dir_names = os.listdir(".")
        return name in dir_names

    def clone_repos(self):
        try:
            file = open(GL_REPO_URLS)

            for url in file.read().split():
                repo_name = os.path.split(url)[1]

                if not self.repo_exists(repo_name) and requests.head(url).status_code == 200:
                    git_clone(url, self.verbose)

        except FileNotFoundError as ex:
            logging.error("An error occurred when reading " + GL_REPO_URLS)
            logging.exception(str(ex))

        finally:
            file.close()

    def start(self):
        # Go to repo directory
        os.chdir(self.repo_dir)

        while True:
            now = datetime.now()
            target_time = now.replace(hour=self.hour, minute=self.minute, second=0)
            seconds_since_target_time = (now - target_time).total_seconds()

            if 0 <= seconds_since_target_time < self.sleep_timer:
                self.clone_repos()

                # Pull all repos after cloning
                pull_all_repos(self.verbose)

            sleep(self.sleep_timer)


def get_arguments() -> Tuple[Namespace, List[str]]:
    """ Parse command line arguments """

    parser = argparse.ArgumentParser(description='Clone and pull repositories')
    parser.add_argument("-t", "--trace", action="store_true", help="Enable tracing output")
    parser.add_argument("-v", "--verbose", action="store_true", help="Enable logs from subprocess")
    parser.add_argument("-hr", "--hour", type=int, default=0, help="Hour, when to execute workers")
    parser.add_argument("-mn", "--minute", type=int, default=0, help="Minute, when to execute workers")

    return parser.parse_known_args()


def main():
    args, unknown = get_arguments()

    if args.trace:
        log_level = logging.DEBUG
    else:
        log_level = logging.WARNING

    logging.basicConfig(format='%(asctime)s - %(levelname)s: %(message)s', level=log_level)

    app = RepositoryUpdater(args.verbose, args.hour, args.minute)
    app.start()


if __name__ == "__main__":
    main()
