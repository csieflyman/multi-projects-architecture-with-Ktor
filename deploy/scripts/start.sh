#!/bin/bash

source version.sh

# run kill -3 $pid to print jvm thread dump and memory usage to jvm.log
export JAVA_OPTS="-Xms128m -Xmx512m -XX:+UnlockDiagnosticVMOptions -XX:+LogVMOutput -XX:LogFile=$HOME/jvm.log"
export APP_OPTS="-Dconfig.file="$appPath/application-$env.conf" -Dlogback.configurationFile="$appPath/logback.xml" -Dswaggerui.dir="$HOME/swagger-ui" -Dkotlinx.coroutines.debug"

echo $JAVA_OPTS APP_OPTS
echo "start..."
$appPath/bin/ktor-example &

