FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

COPY ./target/sgivu-purchase-sale-0.0.1-SNAPSHOT.jar sgivu-purchase-sale.jar

EXPOSE 8084

ENTRYPOINT [ "java", "-jar", "sgivu-purchase-sale.jar" ]