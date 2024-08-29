#!/bin/bash

cd /fetcher-pipeline || exit

if [ -z "$DEBUGPY" ]; then
  echo "Debugpy is not enabled"
  # do something
  # Start scripts
pm2 start "python app.py -i /content ${ADDITIONAL_PARAMS_PIPELINE} -hr $PIPELINE_FREQUENCY_HOUR -mn $PIPELINE_FREQUENCY_MINUTE --queue_exclude $QUEUE_EXCLUDE_ARGS"  --name fetcher-pipeline

pm2 start "python directorycleaner.py ${ADDITIONAL_PARAMS_DIRECTORY_CLEANER} -hr $DIR_CLEANUP_FREQUENCY_HOUR -mn $DIR_CLEANUP_FREQUENCY_MINUTE" --name directory-cleaner
pm2 start "python update_repo.py ${ADDITIONAL_PARAMS_UPDATE_REPO} -hr $UPDATE_REPO_FREQUENCY_HOUR -mn $UPDATE_REPO_FREQUENCY_MINUTE" --name update-rc-respository

# Output log to stdout
pm2 logs
else
  echo "Debugpy is enabled"
  pip install debugpy
  python -m debugpy --wait-for-client --listen 0.0.0.0:5678 app.py -i /content $ADDITIONAL_PARAMS_PIPELINE -hr $PIPELINE_FREQUENCY_HOUR -mn $PIPELINE_FREQUENCY_MINUTE --queue_exclude $QUEUE_EXCLUDE_ARGS
fi



