import argparse
import logging
import os
from argparse import Namespace
from time import sleep
from typing import Tuple, List

import requests
from process_tools import git_clone, pull_all_repos

GL_REPO_URLS = "/repourls/gl_repo_urls.txt"


class RepositoryUpdater:

    def __init__(self, verbose, hour=1, minute=0):
        self.verbose = verbose
        self.hour = hour
        self.minute = minute
        self.repo_dir = os.getenv("ORATURE_REPO_DIR")

    def start(self):
        # Go to repo directory
        os.chdir(self.repo_dir)

        wait_timer = (self.hour * 3600) + (self.minute * 60)

        if wait_timer == 0:
            logging.debug("Set timer to more than zero minutes")
            exit(0)

        while True:
            self.clone_repos()

            # Pull all repos after cloning
            pull_all_repos(self.verbose)

            sleep(wait_timer)

    @staticmethod
    def repo_exists(name):
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


def get_arguments() -> Tuple[Namespace, List[str]]:
    """ Parse command line arguments """

    parser = argparse.ArgumentParser(description='Clone and pull repositories')
    parser.add_argument("-t", "--trace", action="store_true", help="Enable tracing output")
    parser.add_argument("-v", "--verbose", action="store_true", help="Enable logs from subprocess")
    parser.add_argument("-hr", "--hour", type=int, default=0, help="Frequency when to update repos in hours")
    parser.add_argument("-mn", "--minute", type=int, default=0, help="Frequency when to update repos in minutes")

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
