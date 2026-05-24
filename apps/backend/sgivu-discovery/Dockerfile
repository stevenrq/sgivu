FROM amazoncorretto:25-alpine-jdk

WORKDIR /app

COPY ./target/sgivu-discovery-0.1.0.jar sgivu-discovery.jar

EXPOSE 8761

ENTRYPOINT [ "java", "-jar", "sgivu-discovery.jar" ]