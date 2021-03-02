#!/bin/bash

cd /fetcher-pipeline || exit

# Start scripts
pm2 start "python app.py -i /content -t -v -hr $PIPELINE_FREQUENCY_HOUR -mn $PIPELINE_FREQUENCY_MINUTE" --name fetcher-pipeline
pm2 start "python directorycleaner.py -t -hr $DIR_CLEANUP_FREQUENCY_HOUR -mn $DIR_CLEANUP_FREQUENCY_MINUTE" --name directory-cleaner
pm2 start "python update_repo.py -t -v -hr $UPDATE_REPO_FREQUENCY_HOUR -mn $UPDATE_REPO_FREQUENCY_MINUTE" --name update-rc-respository

# Output log to stdout
pm2 logs
