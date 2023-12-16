#!/bin/bash

readonly COMMAND="$1"
readonly TAIL_LOG="$2"

cd "$(dirname $0)/.."
readonly WORKING_DIR="$(pwd)"
readonly LOG_DIR="./logs"
readonly LOG_FILE_NAME="autumn.log"
readonly JAR_FILE="${WORKING_DIR}/target/autumn.jar"
readonly MAIN_CLASS="io.liuwei.autumn.Application"
readonly APP_ARGS="--spring.profiles.active=logfile,prod"

prepare_java_opts() {
    # JVM 选项参考 https://github.com/vipshop/vjtools/blob/master/vjstar/src/main/script/jvm-options/jvm-options.sh
    JAVA_OPTS=""
    if [[ "$(java_major_version)" -ge 9 ]]; then
      # --add-opens 避免出现 "illegal reflective access" 警告，见 https://stackoverflow.com/questions/52185626/illegal-reflective-access-when-i-stop-springboot-web-application-with-tomcat-9-a
      JAVA_OPTS="${JAVA_OPTS} --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED"
    fi
    JAVA_OPTS="${JAVA_OPTS} -Xms256m -Xmx256m -XX:NewRatio=1"
    JAVA_OPTS="${JAVA_OPTS} -XX:-UseBiasedLocking -XX:AutoBoxCacheMax=20000 -Djava.security.egd=file:/dev/./urandom"
    JAVA_OPTS="${JAVA_OPTS} -XX:+PrintCommandLineFlags -XX:-OmitStackTraceInFastThrow -XX:ErrorFile=${LOG_DIR}/hs_err_%p.log"
    JAVA_OPTS="${JAVA_OPTS} -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"
}

start() {
    prepare_java_opts
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

before_start() {
    mvn -v || exit 1

    if [[ ! -d "${LOG_DIR}" ]]; then
        mkdir "${LOG_DIR}" || exit 1
    fi

    touch "${LOG_DIR}/out.log" "${LOG_DIR}/${LOG_FILE_NAME}" || exit 1
}

after_start() {
    ps -ef | grep "${WORKING_DIR}" | grep -v grep

    if [[ "${TAIL_LOG}" != "" ]]; then
        tail -f "${LOG_DIR}/out.log" "${LOG_DIR}/${LOG_FILE_NAME}"
    fi
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

# e.g. 1.8.0_131 -> 8, 9 -> 9, 10.0.1 -> 10 ...
java_major_version() {
    local version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    local major_version=$(echo "${version}" | awk -F. '{print $1}')
    if [[ "${major_version}" -eq 1 ]]; then
        major_version=$(echo "${version}" | awk -F. '{print $2}')
    fi
    echo "${major_version}"
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
