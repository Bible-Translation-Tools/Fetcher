#!/bin/bash

cd /fetcher-pipeline || exit

pm2 start "python3.8 app.py -i /content -t -v -hr 0 -mn 0" --name fetcher-pipeline
pm2 start "python3.8 directorycleaner.py -t -hr $UPDATE_REPO_HOUR -mn $UPDATE_REPO_MINUTE" --name directory-cleaner

pm2 logs
