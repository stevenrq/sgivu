FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

COPY ./target/sgivu-client-0.0.1-SNAPSHOT.jar sgivu-client.jar

EXPOSE 8082

ENTRYPOINT [ "java", "-jar", "sgivu-client.jar" ]