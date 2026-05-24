package com.sgivu.vehicle.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

  @Value("${aws.access.key}")
  private String awsAccessKey;

  @Value("${aws.secret.key}")
  private String awsSecretKey;

  @Value("${aws.region}")
  private String awsRegion;

  @Bean
  S3Client s3Client() {
    AwsCredentials awsCredentials = AwsBasicCredentials.create(awsAccessKey, awsSecretKey);
    return S3Client.builder()
        .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
        .region(Region.of(awsRegion))
        .endpointOverride(URI.create("https://s3.us-east-1.amazonaws.com"))
        .overrideConfiguration(
            ClientOverrideConfiguration.builder()
                .retryPolicy(RetryPolicy.builder().numRetries(1).build())
                .apiCallAttemptTimeout(Duration.ofSeconds(5))
                .apiCallTimeout(Duration.ofSeconds(10))
                .build())
        .build();
  }

  @Bean
  S3Presigner s3Presigner() {
    AwsCredentials awsCredentials = AwsBasicCredentials.create(awsAccessKey, awsSecretKey);
    return S3Presigner.builder()
        .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
        .region(Region.of(awsRegion))
        .build();
  }
}
