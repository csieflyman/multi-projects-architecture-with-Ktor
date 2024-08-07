#!/bin/bash

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/.." >/dev/null
export APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

export APP_LOG_HOME="$APP_HOME/log"

# run kill -3 $pid to print jvm thread dump and memory usage to jvm.log
export JAVA_OPTS="-Xms128m -Xmx256m -XX:+UnlockDiagnosticVMOptions -XX:+LogVMOutput -XX:LogFile=$APP_HOME/jvm.log"
export APP_OPTS=-Dconfig.file="$APP_HOME/application.conf -Dlogback.configurationFile=$APP_HOME/logback.xml -Dproject.config.dir=$APP_HOME -Dswaggerui.dir=$APP_HOME/swagger-ui -Dkotlinx.coroutines.debug -Dotel.javaagent.enabled=true -javaagent:$APP_HOME/lib/opentelemetry-javaagent.jar -Dotel.javaagent.configuration-file=$APP_HOME/otel-javaagent.properties"

export PORT="8080"
export SERVER_SHUTDOWN_KEY="changeit"

export INFRA_DB_URL="jdbc:postgresql://localhost:5432/infra"
export INFRA_DB_USER="infra"
export INFRA_DB_PASSWORD="changeit"
export OPS_DB_URL="jdbc:postgresql://localhost:5432/ops"
export OPS_DB_USER="ops"
export OPS_DB_PASSWORD="changeit"
export CLUB_DB_URL="jdbc:postgresql://localhost:5432/club"
export CLUB_DB_USER="club"
export CLUB_DB_PASSWORD="changeit"

export REDIS_HOST="localhost"
export REDIS_PORT="6379"

export SWAGGER_UI_PATH="$APP_HOME/swagger-ui"
export SWAGGER_UI_AUTH_USER="swagger"
export SWAGGER_UI_AUTH_PASSWORD="changeit"

export GOOGLE_APPLICATION_CREDENTIALS="$APP_HOME/firebase-key.json"

#export SENTRY_DSN=
#export SENDGRID_API_KEY=
#export TWILIO_ACCOUNT_SID=
#export TWILIO_AUTH_TOKEN=
#export TWILIO_FROM_PHONE_NUMBER=
#export LOKI_USERNAME=
#export LOKI_PASSWORD=

# ===== subprojects ops =====
export OPS_AUTH_ROOT_API_KEY="changeit"
export OPS_AUTH_MONITOR_API_KEY="changeit"
export OPS_AUTH_USER_API_KEY="changeit"

# ===== subprojects club =====
export CLUB_AUTH_ROOT_API_KEY="changeit"
export CLUB_AUTH_ANDROID_API_KEY="changeit"
export CLUB_AUTH_ANDROID_RUNAS_KEY="changeit"
export CLUB_AUTH_IOS_API_KEY="changeit"
export CLUB_AUTH_IOS_RUNAS_KEY="changeit"

echo "JAVA_OPTS=$JAVA_OPTS"
echo "APP_OPTS=$APP_OPTS"
"$APP_HOME/bin/app"