FROM openjdk:21

COPY /build/libs/gitlab-1.0.0-SNAPSHOT.jar gitlab.jar
COPY /locale/ /locale/