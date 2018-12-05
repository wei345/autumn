#!/bin/bash

ACT="$1"
cd "$(dirname $0)/.."
DIR="$(pwd)"
JAR_FILE="${DIR}/target/autumn.jar"

do_start() {
    if [[ ! -d "logs" ]]; then
        mkdir logs || exit 1
    fi
    touch logs/out.log logs/autumn.log || exit 1

    local config_file="src/main/resources/application-production.properties"
    rm "${config_file}"
    ln -s "${DIR}/../www/conf/autumn/application-production.properties" "${config_file}"  || exit 1
    mvn clean package || exit 1
    nohup java -jar "${JAR_FILE}" --spring.profiles.active=production &>logs/out.log &
    ps -ef | grep "${JAR_FILE}" | grep -v grep
    tail -f logs/out.log logs/autumn.log
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

