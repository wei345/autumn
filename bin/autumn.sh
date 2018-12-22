#!/bin/bash

ACT="$1"
cd "$(dirname $0)/.."
DIR="$(pwd)"
JAR_FILE="${DIR}/target/autumn.jar"

LD_LIBRARY_PATH="$LD_LIBRARY_PATH:/home/admin/apr/lib"
export LD_LIBRARY_PATH

LOGDIR="./logs"
MEM_OPTS="-Xms150m -Xmx150m -XX:NewRatio=1"
OPTIMIZE_OPTS="-XX:-UseBiasedLocking -XX:AutoBoxCacheMax=20000 -Djava.security.egd=file:/dev/./urandom"
SHOOTING_OPTS="-XX:+PrintCommandLineFlags -XX:-OmitStackTraceInFastThrow -XX:ErrorFile=${LOGDIR}/hs_err_%p.log"
OTHER_OPTS="-Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"
JAVA_OPTS="${MEM_OPTS} ${OPTIMIZE_OPTS} ${SHOOTING_OPTS} ${OTHER_OPTS}"

do_start() {
    if [[ ! -d "${LOGDIR}" ]]; then
        mkdir "${LOGDIR}" || exit 1
    fi
    touch "${LOGDIR}/out.log" "${LOGDIR}/autumn.log" || exit 1

    local config_file="src/main/resources/application-production.properties"
    rm "${config_file}"
    ln -s "${DIR}/../www/conf/autumn/application-production.properties" "${config_file}"  || exit 1
    mvn clean package || exit 1
    nohup java ${JAVA_OPTS} -jar "${JAR_FILE}" --spring.profiles.active=production,logfile &>${LOGDIR}/out.log &
    ps -ef | grep "${JAR_FILE}" | grep -v grep
    tail -f "${LOGDIR}/out.log" "${LOGDIR}/autumn.log"
}

do_stop() {
    local pid="$(ps -ef | grep "${JAR_FILE}" | grep -v grep | awk '{print $2}')"
    if [[ "${pid}" != "" ]]; then
        echo "Killing ${pid}"
        kill -9 "${pid}" || exit 1
    fi
}

case "${ACT}" in
  start)
    do_start
    ;;
  stop)
    do_stop
    ;;
  restart)
    do_stop
    do_start
    ;;
  *)
    echo "Usage: $0 start|stop|restart" >&2
    exit 1
    ;;
esac

