#!/bin/bash

cd /fetcher-pipeline || exit

pm2 start "python3.8 app.py -i /content -t -v -hr $PIPELINE_HOUR -mn $PIPELINE_MINUTE" --name fetcher-pipeline
pm2 start "python3.8 directorycleaner.py -t -hr $DIR_CLEANUP_HOUR -mn $DIR_CLEANUP_MINUTE" --name directory-cleaner
pm2 start "python3.8 update_repo.py -t -v -hr $UPDATE_REPO_HOUR -mn $UPDATE_REPO_MINUTE" --name update-rc-respository

pm2 logs
