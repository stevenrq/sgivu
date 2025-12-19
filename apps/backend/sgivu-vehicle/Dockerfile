FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

COPY ./target/sgivu-vehicle-0.0.1-SNAPSHOT.jar sgivu-vehicle.jar

EXPOSE 8083

ENTRYPOINT [ "java", "-jar", "sgivu-vehicle.jar" ]