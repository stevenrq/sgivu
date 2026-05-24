FROM amazoncorretto:25-alpine-jdk

WORKDIR /app

COPY ./target/sgivu-gateway-0.1.0.jar sgivu-gateway.jar

EXPOSE 8080

ENTRYPOINT [ "java", "-jar", "sgivu-gateway.jar" ]