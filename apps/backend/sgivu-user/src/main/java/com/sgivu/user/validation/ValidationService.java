package com.sgivu.user.validation;

import com.sgivu.user.dto.ApiResponse;
import com.sgivu.user.dto.UserResponse;
import com.sgivu.user.dto.UserUpdateRequest;
import com.sgivu.user.entity.User;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

/**
 * Valida reglas de negocio del dominio de usuarios. Complementa Bean Validation con restricciones
 * de longitud para cédula y teléfono exigidas por contratos y notificaciones.
 */
@Service
public class ValidationService {

  /** Orquesta validaciones propias y de Spring; devuelve respuesta 400 cuando hay errores. */
  public <T> ResponseEntity<ApiResponse<UserResponse>> handleValidation(
      T target, BindingResult bindingResult) {
    if (!(target instanceof User || target instanceof UserUpdateRequest)) {
      throw new IllegalArgumentException("Tipo de objetivo no válido");
    }

    Map<String, String> errors = validate(target);

    if (!errors.isEmpty()) {
      return validationError(errors);
    }

    if (bindingResult.hasErrors()) {
      return validate(bindingResult);
    }

    return null;
  }

  /** Devuelve 400 con el mapa de errores acumulados. */
  public <T> ResponseEntity<ApiResponse<T>> validationError(Map<String, String> errors) {
    ApiResponse<T> apiResponse = new ApiResponse<>(errors);
    return ResponseEntity.badRequest().body(apiResponse);
  }

  /** Adapta los errores de Spring a la estructura genérica de la API. */
  public <T> ResponseEntity<ApiResponse<T>> validate(BindingResult bindingResult) {
    Map<String, String> errors = new HashMap<>();

    bindingResult
        .getFieldErrors()
        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

    return validationError(errors);
  }

  public boolean isValidLength(Number number, int minLength, int maxLength) {
    if (number == null) {
      return true;
    }
    int length = String.valueOf(number).length();
    return length >= minLength && length <= maxLength;
  }

  /**
   * Aplica reglas de longitud para cédula y teléfono según si es alta (User) o actualización
   * (UserUpdateRequest).
   */
  public Map<String, String> validate(Object user) {
    Map<String, String> validationsMessage = new HashMap<>();
    Long nationalId = 0L;
    Long phoneNumber;

    if (user instanceof User u) {
      nationalId = u.getNationalId();
      phoneNumber = u.getPhoneNumber();
    } else if (user instanceof UserUpdateRequest ur) {
      // En actualización solo validamos teléfono porque la cédula no es editable.
      phoneNumber = ur.getPhoneNumber();
    } else {
      throw new IllegalArgumentException("Tipo de usuario no válido");
    }

    if (!isValidLength(nationalId, 7, 10) && user instanceof User) {
      validationsMessage.put("nationalId", "La cédula debe tener entre 7 y 10 dígitos");
    } else if (!isValidLength(phoneNumber, 10, 10)) {
      validationsMessage.put("phoneNumber", "El número de teléfono debe tener 10 dígitos");
    }

    return validationsMessage;
  }
}
