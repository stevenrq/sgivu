package com.sgivu.user.validation;

import com.sgivu.user.validation.annotation.NoSpecialCharacters;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Evita caracteres especiales en campos clave (usuario/nombres) para mantener interoperabilidad con
 * sistemas externos y políticas de seguridad.
 */
public class NoSpecialCharactersValidator
    implements ConstraintValidator<NoSpecialCharacters, String> {

  /**
   * Verifica que el valor contenga únicamente caracteres alfanuméricos.
   *
   * @param value cadena a validar.
   * @param context contexto de Bean Validation.
   * @return {@code true} si es nula o cumple el patrón; {@code false} en caso contrario.
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    return value.matches("^[a-zA-Z0-9]*$");
  }
}
