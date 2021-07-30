#!/bin/bash

source ~/.bash_profile

KTOR_ENV="prod"
KTOR_APP_VERSION="1.0.0"
KTOR_APP_DIR="fanpoll-$KTOR_APP_VERSION-$KTOR_ENV"
KTOR_APP_PATH="$HOME/$KTOR_APP_DIR"
KTOR_LOG_PATH="$KTOR_APP_PATH"/logs
KTOR_SWAGGER_PATH=$HOME/swagger-ui

echo "========== $KTOR_APP_PATH =========="

