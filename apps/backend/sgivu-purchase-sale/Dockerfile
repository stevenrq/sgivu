FROM amazoncorretto:25-alpine-jdk

WORKDIR /app

COPY ./target/sgivu-purchase-sale-0.1.0.jar sgivu-purchase-sale.jar

EXPOSE 8084

ENTRYPOINT [ "java", "-jar", "sgivu-purchase-sale.jar" ]