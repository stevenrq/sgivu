FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

COPY ./target/sgivu-discovery-0.0.1-SNAPSHOT.jar sgivu-discovery.jar

EXPOSE 8761

ENTRYPOINT [ "java", "-jar", "sgivu-discovery.jar" ]