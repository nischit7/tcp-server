# Base image comes from https://hub.docker.com/r/adoptopenjdk/openjdk11/
FROM adoptopenjdk/openjdk11:x86_64-alpine-jdk11u-nightly-slim

RUN apk add bash

EXPOSE 4000

ENV SVC_HOME=/opt/example

RUN mkdir -p $SVC_HOME && \
    addgroup -g 1000 example && \
    adduser -D --home $SVC_HOME --disabled-password --uid 1000 --ingroup example example && \
    mkdir $SVC_HOME/lib $SVC_HOME/conf /var/log/example && \
    chown -R example:example $SVC_HOME /var/log/example

WORKDIR $SVC_HOME

# Switching to service user (non root)
USER example

COPY --chown=example bin/start.sh ${SVC_HOME}/
RUN chmod +x start.sh

COPY --chown=example ./target/simple-tcp-server-1.0-SNAPSHOT.jar ${SVC_HOME}/simple-tcp-server-1.0-SNAPSHOT.jar

ENTRYPOINT ["./start.sh"]
