import os
import requests
from time import sleep

GIT_CLONE = "git clone {}"
PULL_ALL_CMD = "find . -mindepth 1 -maxdepth 1 -type d -print -exec git -C {} pull \;"
GL_REPO_URLS = "gl_repo_urls.txt"

class RepositoryUpdater:

    def __init__(self):
        self.repo_dir = os.getenv("ORATURE_REPO_DIR")
        self.sleep_timer = os.getenv("REFRESH_INTERVAL_HRS") * 3600

    def clone_repos(self):
        try:
            file = open(GL_REPO_URLS)

            for url in file.read().split():
                request = requests.head(url)

                if request.status_code == 200:
                    cmd = GIT_CLONE.format(url)
                    os.system(cmd)

        except FileNotFoundError:
            print("An error occurred when reading file: " + file)

        finally:
            file.close()

    def start(self):
    	# Go to repo directory
        os.chdir(self.repo_dir)

        while True:
            self.clone_repos()

            # Pull all repos after cloning
            os.system(PULL_ALL_CMD)
            
            sleep(self.sleep_timer)

def main():
    app = RepositoryUpdater()
    app.start()