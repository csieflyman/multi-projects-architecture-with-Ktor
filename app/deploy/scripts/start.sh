#!/bin/bash

source configs.sh

# run kill -3 $pid to print jvm thread dump and memory usage to jvm.log
export JAVA_OPTS="-Xms128m -Xmx512m -XX:+UnlockDiagnosticVMOptions -XX:+LogVMOutput -XX:LogFile=$KTOR_LOG_PATH/jvm.log"
export APP_OPTS="-Dconfig.file=$KTOR_APP_PATH/application.conf -Dlogback.configurationFile=$KTOR_APP_PATH/logback.xml -Dproject.config.dir=$KTOR_APP_PATH -Dswaggerui.dir=$KTOR_SWAGGER_PATH -Dkotlinx.coroutines.debug"

echo "$JAVA_OPTS" "$APP_OPTS"
echo "start..."
"$KTOR_APP_PATH"/bin/app &

