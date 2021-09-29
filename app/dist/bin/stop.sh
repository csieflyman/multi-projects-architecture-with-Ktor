#!/bin/bash

echo "stop app..."
curl "http://localhost:$PORT/ops/server/shutdown/$SERVER_SHUTDOWN_KEY"