# Use this Dockerfile to run / debug test inside a Linux container.
#
# docker build -t camel-idea-plugin .
# docker run -it -p5005:5005 camel-idea-plugin /bin/bash
# ./gradlew clean test

FROM openjdk:8
ADD . /usr/src/camel_idea_plugin
WORKDIR /usr/src/camel_idea_plugin