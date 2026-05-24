FROM amazoncorretto:25-alpine-jdk

WORKDIR /app

COPY ./target/sgivu-auth-0.1.0.jar sgivu-auth.jar

EXPOSE 9000

ENTRYPOINT [ "java", "-jar", "sgivu-auth.jar" ]