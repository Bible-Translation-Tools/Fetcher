#!/bin/bash

cd /fetcher-pipeline || exit

if [ -z "$DEPLOY_ENV" ]; then
  echo "Error: Please set the 'DEPLOY_ENV' environment variable."
  exit 1
fi


if [ -z "$OP_SERVICE_ACCOUNT_TOKEN" ]; then
  echo "Error: Please set the 'OP_SERVICE_ACCOUNT_TOKEN' environment variable."
  exit 1
fi

shopt -s expand_aliases

alias op="docker run --rm -e OP_SERVICE_ACCOUNT_TOKEN 1password/op:2 op"

export OP_SERVICE_ACCOUNT_TOKEN=$OP_SERVICE_ACCOUNT_TOKEN

export SERVICE_BUS_CONNECTION_STRING=$(op read "op://AppDev Scripture Accessibility/languageapi-bus-send-connstring/$DEPLOY_ENV/conn_string")


# Start scripts
pm2 start "python app.py -i /content ${ADDITIONAL_PARAMS_PIPELINE} -hr $PIPELINE_FREQUENCY_HOUR -mn $PIPELINE_FREQUENCY_MINUTE --queue_exclude $QUEUE_EXCLUDE_ARGS"  --name fetcher-pipeline

pm2 start "python directorycleaner.py ${ADDITIONAL_PARAMS_DIRECTORY_CLEANER} -hr $DIR_CLEANUP_FREQUENCY_HOUR -mn $DIR_CLEANUP_FREQUENCY_MINUTE" --name directory-cleaner
pm2 start "python update_repo.py ${ADDITIONAL_PARAMS_UPDATE_REPO} -hr $UPDATE_REPO_FREQUENCY_HOUR -mn $UPDATE_REPO_FREQUENCY_MINUTE" --name update-rc-respository

# Output log to stdout
pm2 logs
