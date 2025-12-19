package com.sgivu.vehicle.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Configura beans de {@link S3Client} y {@link S3Presigner} utilizando credenciales externas. Se
 * reutiliza por los servicios de almacenamiento para generar URLs prefirmadas.
 *
 * <p>Centraliza credenciales y región de AWS para que la capa de dominio no dependa de detalles de
 * infraestructura al gestionar imágenes de inventario.
 */
@Configuration
public class S3Config {

  @Value("${aws.access.key}")
  private String awsAccessKey;

  @Value("${aws.secret.key}")
  private String awsSecretKey;

  @Value("${aws.region}")
  private String awsRegion;

  /**
   * @return cliente síncrono de S3.
   */
  @Bean
  S3Client s3Client() {
    AwsCredentials awsCredentials = AwsBasicCredentials.create(awsAccessKey, awsSecretKey);
    return S3Client.builder()
        .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
        .region(Region.of(awsRegion))
        .endpointOverride(URI.create("https://s3.us-east-1.amazonaws.com"))
        .build();
  }

  /**
   * @return presigner para generar URLs PUT/GET.
   */
  @Bean
  S3Presigner s3Presigner() {
    AwsCredentials awsCredentials = AwsBasicCredentials.create(awsAccessKey, awsSecretKey);
    return S3Presigner.builder()
        .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
        .region(Region.of(awsRegion))
        .build();
  }
}
