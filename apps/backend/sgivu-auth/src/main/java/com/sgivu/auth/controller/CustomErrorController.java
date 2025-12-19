package com.sgivu.auth.controller;

import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

/**
 * Reemplaza la página HTML de Spring Boot en {@code /error} por una respuesta JSON consistente
 * consumible por el portal Angular y otros microservicios.
 */
@RestController
public class CustomErrorController implements ErrorController {

  private final ErrorAttributes errorAttributes;

  public CustomErrorController(ErrorAttributes errorAttributes) {
    this.errorAttributes = errorAttributes;
  }

  @RequestMapping("/error")
  public ResponseEntity<Map<String, Object>> handleError(WebRequest webRequest) {

    ErrorAttributeOptions options =
        ErrorAttributeOptions.of(
            ErrorAttributeOptions.Include.PATH,
            ErrorAttributeOptions.Include.EXCEPTION,
            ErrorAttributeOptions.Include.MESSAGE,
            ErrorAttributeOptions.Include.BINDING_ERRORS,
            ErrorAttributeOptions.Include.ERROR,
            ErrorAttributeOptions.Include.STATUS);

    Map<String, Object> errors = errorAttributes.getErrorAttributes(webRequest, options);

    return ResponseEntity.status((int) errors.getOrDefault("status", 500)).body(errors);
  }
}
