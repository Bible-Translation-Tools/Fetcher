#!/bin/bash

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
docker-compose down
docker-compose build fileserver
docker-compose pull fetcher-app pipeline
docker-compose up



