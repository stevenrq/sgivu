FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

COPY ./target/sgivu-config-0.0.1-SNAPSHOT.jar sgivu-config.jar

EXPOSE 8888

ENTRYPOINT [ "java", "-jar", "sgivu-config.jar" ]