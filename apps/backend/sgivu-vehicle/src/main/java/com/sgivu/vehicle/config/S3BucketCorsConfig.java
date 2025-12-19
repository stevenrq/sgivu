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
 * Configura CORS en el bucket de S3 usado para almacenar las imágenes.
 *
 * <p>El frontend sube los archivos directamente al bucket mediante URLs prefirmadas, por lo que el
 * bucket debe permitir peticiones cross-origin desde el/los dominios del frontend.
 *
 * <p>Automatiza la política CORS en arranque para evitar fallos de carga en la vitrina y en los
 * clientes móviles que consumen URLs prefirmadas.
 */
@Component
public class S3BucketCorsConfig {

  private static final Logger logger = LoggerFactory.getLogger(S3BucketCorsConfig.class);

  private final S3Client s3Client;
  private final String bucket;
  private final List<String> allowedOrigins;

  /**
   * Construye el componente con dependencias externas y orígenes permitidos.
   *
   * @param s3Client cliente configurado para el bucket
   * @param bucket nombre del bucket de imágenes de vehículos
   * @param allowedOrigins lista separada por comas con orígenes autorizados
   */
  public S3BucketCorsConfig(
      S3Client s3Client,
      @Value("${aws.s3.vehicles-bucket}") String bucket,
      @Value("${aws.s3.allowed-origins:*}") String allowedOrigins) {
    this.s3Client = s3Client;
    this.bucket = bucket;
    this.allowedOrigins = parseOrigins(allowedOrigins);
  }

  /**
   * Verifica al iniciar si el bucket ya tiene la regla CORS necesaria y, de no existir, la crea o
   * actualiza. Protege la subida desde frontends mediante URLs prefirmadas.
   */
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
          "CORS ya configurado en bucket {} para orígenes {}",
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
          "No se encontró configuración CORS previa para el bucket {}: {}",
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
          "CORS actualizado en bucket {} para orígenes {}",
          bucket,
          String.join(",", allowedOrigins));
    } catch (S3Exception e) {
      logger.error(
          "No se pudo configurar CORS en bucket {}: {}",
          bucket,
          e.awsErrorDetails().errorMessage());
    }
  }

  /**
   * Revisa si la configuración actual del bucket ya incluye la regla objetivo para evitar
   * sobreescrituras innecesarias.
   *
   * @param targetRule regla esperada
   * @return {@code true} si la regla ya está configurada
   */
  private boolean isRuleAlreadyPresent(CORSRule targetRule) {
    try {
      var existing = s3Client.getBucketCors(GetBucketCorsRequest.builder().bucket(bucket).build());
      return existing.corsRules().stream().anyMatch(rule -> matchesRule(rule, targetRule));
    } catch (S3Exception e) {
      return false;
    }
  }

  /**
   * Compara reglas CORS asegurando que métodos, orígenes y encabezados cumplan con la regla
   * esperada.
   *
   * @param existing regla actual en el bucket
   * @param expected regla deseada
   * @return {@code true} si la regla actual cubre lo esperado
   */
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

  /**
   * Normaliza la lista de orígenes permitidos a partir de la propiedad separada por comas.
   *
   * @param raw valor crudo de configuración
   * @return lista de orígenes; si está vacía, retorna comodín
   */
  private List<String> parseOrigins(String raw) {
    List<String> origins =
        Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    return origins.isEmpty() ? List.of("*") : origins;
  }
}
