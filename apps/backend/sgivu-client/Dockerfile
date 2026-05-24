FROM amazoncorretto:25-alpine-jdk

WORKDIR /app

COPY ./target/sgivu-client-0.1.0.jar sgivu-client.jar

EXPOSE 8082

ENTRYPOINT [ "java", "-jar", "sgivu-client.jar" ]