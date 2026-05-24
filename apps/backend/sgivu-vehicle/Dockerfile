FROM amazoncorretto:25-alpine-jdk

WORKDIR /app

COPY ./target/sgivu-vehicle-0.1.0.jar sgivu-vehicle.jar

EXPOSE 8083

ENTRYPOINT [ "java", "-jar", "sgivu-vehicle.jar" ]