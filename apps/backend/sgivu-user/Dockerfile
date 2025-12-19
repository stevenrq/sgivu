FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

COPY ./target/sgivu-user-0.0.1-SNAPSHOT.jar sgivu-user.jar

EXPOSE 8081

ENTRYPOINT [ "java", "-jar", "sgivu-user.jar" ]