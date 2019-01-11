#!/bin/bash

cd "$(dirname $0)/.."
readonly COMMAND="$1"

readonly WORKING_DIR="$(pwd)"
readonly LOG_DIR="./logs"
readonly JAR_FILE="${WORKING_DIR}/target/autumn.jar"
readonly MAIN_CLASS="xyz.liuw.autumn.Application"
readonly APP_ARGS="--spring.profiles.include=logfile"

# e.g. 1.8.0_131 -> 8, 9 -> 9, 10.0.1 -> 10 ...
java_major_version() {
    local version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    local major_version=$(echo "${version}" | awk -F. '{print $1}')
    if [[ "${major_version}" -eq 1 ]]; then
        major_version=$(echo "${version}" | awk -F. '{print $2}')
    fi
    echo "${major_version}"
}

# JVM 选项参考 https://github.com/vipshop/vjtools/blob/master/vjstar/src/main/script/jvm-options/jvm-options.sh
readonly MEM_OPTS="-Xms150m -Xmx150m -XX:NewRatio=1"
readonly OPTIMIZE_OPTS="-XX:-UseBiasedLocking -XX:AutoBoxCacheMax=20000 -Djava.security.egd=file:/dev/./urandom"
readonly SHOOTING_OPTS="-XX:+PrintCommandLineFlags -XX:-OmitStackTraceInFastThrow -XX:ErrorFile=${LOG_DIR}/hs_err_%p.log"
readonly OTHER_OPTS="-Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"
if [[ "$(java_major_version)" -ge 9 ]]; then
  # --add-opens 避免出现 "illegal reflective access" 警告，见 https://stackoverflow.com/questions/52185626/illegal-reflective-access-when-i-stop-springboot-web-application-with-tomcat-9-a
  readonly ADD_OPENS="--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED"
else
  readonly ADD_OPENS=""
fi
readonly JAVA_OPTS="${ADD_OPENS} ${MEM_OPTS} ${OPTIMIZE_OPTS} ${SHOOTING_OPTS} ${OTHER_OPTS}"

before_start() {
    mvn -v || exit 1

    if [[ ! -d "${LOG_DIR}" ]]; then
        mkdir "${LOG_DIR}" || exit 1
    fi

    touch "${LOG_DIR}/out.log" "${LOG_DIR}/autumn.log" || exit 1
}

quick_start() {
    local classpath_file="target/classpath.txt"

    mvn clean compile dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=${classpath_file} || exit 1

    local classpath="${WORKING_DIR}/target/classes:$(cat ${classpath_file})" || exit 1

    nohup java ${JAVA_OPTS} -classpath "${classpath}" ${MAIN_CLASS} ${APP_ARGS} &>"${LOG_DIR}/out.log" &
}

jar_start() {
    mvn clean package || exit 1

    nohup java ${JAVA_OPTS} -jar "${JAR_FILE}" ${APP_ARGS} &>"${LOG_DIR}/out.log" &
}

after_start() {
    ps -ef | grep "${WORKING_DIR}" | grep -v grep

    tail -f "${LOG_DIR}/out.log" "${LOG_DIR}/autumn.log"
}

start() {
    before_start
    quick_start
    after_start
}

stop() {
    ps -ef | grep "${WORKING_DIR}" | grep -v grep | awk '{print $2}' | while read pid; do
        if [[ "${pid}" != "" ]]; then
            kill -9 "${pid}" || exit 1
            echo "Killed ${pid}"
        fi
    done
}

case "${COMMAND}" in
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
