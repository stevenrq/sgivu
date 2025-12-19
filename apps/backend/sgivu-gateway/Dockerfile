FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

COPY ./target/sgivu-gateway-0.0.1-SNAPSHOT.jar sgivu-gateway.jar

EXPOSE 8080

ENTRYPOINT [ "java", "-jar", "sgivu-gateway.jar" ]