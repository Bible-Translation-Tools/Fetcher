#!/bin/bash
set -e

cd /fetcher-pipeline

if [ -z "$DEBUGPY" ]; then
  echo "Debugpy is not enabled"
  exec supervisord -c /etc/supervisor/fetcher.conf
else
  echo "Debugpy is enabled"
  # shellcheck disable=SC2086 # params are intentionally word-split
  exec python -m debugpy --wait-for-client --listen 0.0.0.0:5678 app.py -i /content $ADDITIONAL_PARAMS_PIPELINE \
    -hr "$PIPELINE_FREQUENCY_HOUR" -mn "$PIPELINE_FREQUENCY_MINUTE" --queue_exclude $QUEUE_EXCLUDE_ARGS
fi
