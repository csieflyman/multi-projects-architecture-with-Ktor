#!/bin/bash

source version.sh

# replace files
if [ -f $appDir.zip ]; then
     rm -rf $appDir
     unzip -o $appDir.zip
     mv $appDir.zip $appDir.bak.zip
fi
