FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

COPY ./target/sgivu-auth-0.0.1-SNAPSHOT.jar sgivu-auth.jar

EXPOSE 9000

ENTRYPOINT [ "java", "-jar", "sgivu-auth.jar" ]