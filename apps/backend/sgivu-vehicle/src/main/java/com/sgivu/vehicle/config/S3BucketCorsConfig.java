package com.sgivu.vehicle.config;

import jakarta.annotation.PostConstruct;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.GetBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Configuración automática de CORS para el bucket S3 de vehículos. Permite solicitudes desde los
 * orígenes especificados en la configuración de la aplicación.
 */
@Component
public class S3BucketCorsConfig {

  private static final Logger logger = LoggerFactory.getLogger(S3BucketCorsConfig.class);

  private final S3Client s3Client;
  private final String bucket;
  private final List<String> allowedOrigins;

  public S3BucketCorsConfig(
      S3Client s3Client,
      @Value("${aws.s3.vehicles-bucket}") String bucket,
      @Value("${aws.s3.allowed-origins:*}") String allowedOrigins) {
    this.s3Client = s3Client;
    this.bucket = bucket;
    this.allowedOrigins = parseOrigins(allowedOrigins);
  }

  @PostConstruct
  public void ensureBucketCors() {
    CORSRule uploadRule =
        CORSRule.builder()
            .allowedMethods("GET", "PUT", "POST", "DELETE", "HEAD")
            .allowedOrigins(allowedOrigins)
            .allowedHeaders(List.of("*"))
            .exposeHeaders(List.of("ETag"))
            .maxAgeSeconds(3600)
            .build();

    if (isRuleAlreadyPresent(uploadRule)) {
      logger.info(
          "CORS already configured on bucket {} for origins {}",
          bucket,
          String.join(",", allowedOrigins));
      return;
    }

    List<CORSRule> rules = new ArrayList<>();
    try {
      var existing = s3Client.getBucketCors(GetBucketCorsRequest.builder().bucket(bucket).build());
      rules.addAll(existing.corsRules());
    } catch (S3Exception e) {
      logger.warn(
          "No prior CORS configuration found for bucket {}: {}",
          bucket,
          e.awsErrorDetails().errorMessage());
    }

    rules.add(uploadRule);

    try {
      s3Client.putBucketCors(
          PutBucketCorsRequest.builder()
              .bucket(bucket)
              .corsConfiguration(CORSConfiguration.builder().corsRules(rules).build())
              .build());
      logger.info(
          "CORS updated on bucket {} for origins {}", bucket, String.join(",", allowedOrigins));
    } catch (S3Exception e) {
      logger.error(
          "Could not configure CORS on bucket {}: {}", bucket, e.awsErrorDetails().errorMessage());
    }
  }

  private boolean isRuleAlreadyPresent(CORSRule targetRule) {
    try {
      var existing = s3Client.getBucketCors(GetBucketCorsRequest.builder().bucket(bucket).build());
      return existing.corsRules().stream().anyMatch(rule -> matchesRule(rule, targetRule));
    } catch (S3Exception e) {
      return false;
    }
  }

  private boolean matchesRule(CORSRule existing, CORSRule expected) {
    var expectedMethods =
        expected.allowedMethods().stream().map(m -> m.toUpperCase(Locale.ROOT)).toList();
    boolean methodsOk =
        new HashSet<>(
                existing.allowedMethods().stream().map(m -> m.toUpperCase(Locale.ROOT)).toList())
            .containsAll(expectedMethods);
    boolean originsOk =
        new HashSet<>(existing.allowedOrigins()).containsAll(expected.allowedOrigins());
    boolean headersOk =
        existing.allowedHeaders().contains("*")
            || new HashSet<>(existing.allowedHeaders()).containsAll(expected.allowedHeaders());
    return methodsOk && originsOk && headersOk;
  }

  private List<String> parseOrigins(String raw) {
    List<String> origins =
        Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    return origins.isEmpty() ? List.of("*") : origins;
  }
}
