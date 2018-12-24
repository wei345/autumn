#!/bin/bash

ACT="$1"
cd "$(dirname $0)/.."
DIR="$(pwd)"
JAR_FILE="${DIR}/target/autumn.jar"
MAIN_CLASS="xyz.liuw.autumn.Application"
APP_ARGS="--spring.profiles.active=production,logfile"

LD_LIBRARY_PATH="$LD_LIBRARY_PATH:/home/admin/apr/lib"
export LD_LIBRARY_PATH

LOGDIR="./logs"
MEM_OPTS="-Xms150m -Xmx150m -XX:NewRatio=1"
OPTIMIZE_OPTS="-XX:-UseBiasedLocking -XX:AutoBoxCacheMax=20000 -Djava.security.egd=file:/dev/./urandom"
SHOOTING_OPTS="-XX:+PrintCommandLineFlags -XX:-OmitStackTraceInFastThrow -XX:ErrorFile=${LOGDIR}/hs_err_%p.log"
OTHER_OPTS="-Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"
JAVA_OPTS="${MEM_OPTS} ${OPTIMIZE_OPTS} ${SHOOTING_OPTS} ${OTHER_OPTS}"

before_start() {
    mvn -v

    if [[ ! -d "${LOGDIR}" ]]; then
        mkdir "${LOGDIR}" || exit 1
    fi
    touch "${LOGDIR}/out.log" "${LOGDIR}/autumn.log" || exit 1

    local config_file="src/main/resources/application-production.properties"
    rm "${config_file}"
    ln -s "${DIR}/../www/conf/autumn/application-production.properties" "${config_file}"  || exit 1
}

after_start() {
    ps -ef | grep "${DIR}" | grep -v grep
    tail -f "${LOGDIR}/out.log" "${LOGDIR}/autumn.log"
}

jar_start() {
    mvn clean package || exit 1
    nohup java ${JAVA_OPTS} -jar "${JAR_FILE}" ${APP_ARGS} &>${LOGDIR}/out.log &
}

quick_start() {
    local classpath_file="target/classpath.txt"
    mvn -Dmdep.outputFile=${classpath_file} -DincludeScope=runtime clean compile dependency:build-classpath || exit 1
    local classpath="${DIR}/target/classes:$(cat ${classpath_file})" || exit 1
    nohup java ${JAVA_OPTS} -classpath ${classpath} ${MAIN_CLASS} ${APP_ARGS} &>${LOGDIR}/out.log &
}

start() {
    before_start
    quick_start
    after_start
}

stop() {
    ps -ef | grep "${DIR}" | grep -v grep | awk '{print $2}' | while read pid; do
        if [[ "${pid}" != "" ]]; then
            echo "Killing ${pid}"
            kill -9 "${pid}" || exit 1
        fi
    done
}

case "${ACT}" in
  start)
    start
    ;;
  stop)
    stop
    ;;
  restart)
    stop
    start
    ;;
  *)
    echo "Usage: $0 start|stop|restart" >&2
    exit 1
    ;;
esac

