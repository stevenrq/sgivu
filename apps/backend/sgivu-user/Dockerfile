FROM amazoncorretto:25-alpine-jdk

WORKDIR /app

COPY ./target/sgivu-user-0.1.0.jar sgivu-user.jar

EXPOSE 8081

ENTRYPOINT [ "java", "-jar", "sgivu-user.jar" ]