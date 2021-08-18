# syntax = docker/dockerfile:experimental
# build stage
FROM maven:3.6.2-jdk-11-slim as builder
RUN mkdir -p /usr/src/app
COPY . /usr/src/app
WORKDIR /usr/src/app
#RUN ./installNistLibs.sh

ARG NEXUS_USER
ARG NEXUS_PASSWORD

RUN mvn --settings ./.m2/settings.xml  clean package -DskipTests=true

# create Image stage
FROM adoptopenjdk/openjdk11-openj9:x86_64-ubuntu-jdk-11.0.9_11_openj9-0.23.0-slim

VOLUME /tmp

COPY --from=builder  /usr/src/app/target/nist-validator*.jar ./nist-validator.jar

RUN sh -c 'touch ./nist-validator.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","./nist-validator.jar"]
