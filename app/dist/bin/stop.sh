#!/bin/bash

echo "stop app..."
curl "http://localhost:$PORT/server/shutdown/$SERVER_SHUTDOWN_KEY"