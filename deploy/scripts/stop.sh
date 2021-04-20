#!/bin/bash

source version.sh

echo "stop..."
curl "http://localhost:$APP_SERVER_PORT/server/shutdown/$APP_SERVER_SHUTDOWN_KEY"

