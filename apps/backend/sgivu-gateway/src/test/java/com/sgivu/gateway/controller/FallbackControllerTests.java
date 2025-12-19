package com.sgivu.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

class FallbackControllerTests {

  private final WebTestClient webTestClient =
      WebTestClient.bindToController(new FallbackController()).build();

  @ParameterizedTest
  @MethodSource("fallbackRoutes")
  void shouldReturnServiceUnavailableWithConsistentMessage(
      String path, String expectedDescriptionFragment) {
    webTestClient
        .get()
        .uri(path)
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        .expectBody(String.class)
        .value(
            body -> {
              assertThat(body).contains(expectedDescriptionFragment);
              assertThat(body).contains("Inténtelo de nuevo más tarde.");
            });
  }

  @Test
  void mlFallbackShouldAllowPostRequests() {
    webTestClient
        .post()
        .uri("/fallback/ml")
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        .expectBody(String.class)
        .value(
            body -> {
              assertThat(body).contains("predicción");
              assertThat(body).contains("Inténtelo de nuevo más tarde.");
            });
  }

  private static Stream<Arguments> fallbackRoutes() {
    return Stream.of(
        Arguments.of("/fallback/auth", "autenticación"),
        Arguments.of("/fallback/user", "usuarios"),
        Arguments.of("/fallback/client", "clientes"),
        Arguments.of("/fallback/vehicle", "vehículos"),
        Arguments.of("/fallback/purchase-sale", "compra-venta"),
        Arguments.of("/fallback/ml", "predicción"));
  }
}
