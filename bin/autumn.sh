#!/bin/bash

ACT="$1"
cd "$(dirname $0)/.."
DIR="$(pwd)"
JAR_FILE="${DIR}/target/autumn.jar"

do_start() {
    cp ../www/conf/autumn/application-production.properties src/main/resources/application-production.properties
    mvn clean package
    nohup java -jar "${JAR_FILE}" --spring.profiles.active=production &>/dev/null &
    ps -ef | grep "${JAR_FILE}" | grep -v grep
    tail -f logs/autumn.log
}

do_stop() {
    ps -ef | grep "${JAR_FILE}" | grep -v grep | awk '{print $2}' | xargs kill -9
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

