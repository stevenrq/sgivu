FROM amazoncorretto:25-alpine-jdk

WORKDIR /app

COPY ./target/sgivu-config-0.1.0.jar sgivu-config.jar

EXPOSE 8888

ENTRYPOINT [ "java", "-jar", "sgivu-config.jar" ]