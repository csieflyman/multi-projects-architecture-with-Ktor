#!/bin/bash

echo "stop..."
curl "http://localhost:$SERVER_PORT/server/shutdown/$SERVER_SHUTDOWN_KEY"

