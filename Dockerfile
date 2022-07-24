FROM openjdk:17-jdk-slim-buster

COPY "build/libs/heart-beat-live-server-0.0.1-SNAPSHOT.jar" /heartbeatlive.jar

ENTRYPOINT java -jar /heartbeatlive.jar