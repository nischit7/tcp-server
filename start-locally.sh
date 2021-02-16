#!/usr/bin/env bash

# The home directory of the script will be treated as SVC_HOME
SCRIPTPATH="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
SVC_HOME=${SCRIPTPATH}

# We will dump the heap in the same directory. Alter as needed.
HEAP_DUMP_DIR=${SVC_HOME}

JVM_OPTS="${JVM_OPTS:-} -XX:+HeapDumpOnOutOfMemoryError -XX:StartFlightRecording -XX:HeapDumpPath=${HEAP_DUMP_DIR} \
  -XX:ErrorFile=${HEAP_DUMP_DIR}/hs_err.log"

# Location of numbers.log with be same as SVC_HOME. Override as needed
NUMBERS_LOG_FILE=${SVC_HOME}/numbers.log

# Service log directory. Change as needed
export LOG_DIR=${SVC_HOME}

exec java ${JVM_OPTS} -DLOG_DIR=${LOG_DIR} -jar ${SVC_HOME}/target/simple-tcp-server-1.0-SNAPSHOT.jar --dest.file.absolute.path=${NUMBERS_LOG_FILE}