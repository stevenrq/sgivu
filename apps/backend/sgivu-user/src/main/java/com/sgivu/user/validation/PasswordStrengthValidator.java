package com.sgivu.user.validation;

import com.sgivu.user.validation.annotation.PasswordStrength;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Verifica que la contraseña cumpla el estándar mínimo usado en SGIVU para proteger accesos a
 * operaciones sensibles (contratos, ventas).
 */
public class PasswordStrengthValidator implements ConstraintValidator<PasswordStrength, String> {

  /**
   * Evalúa la contraseña contra el patrón mínimo exigido (minúscula, mayúscula, dígito y carácter
   * especial).
   *
   * @param value valor de la contraseña recibido.
   * @param context contexto de Bean Validation.
   * @return {@code true} si cumple la política o es nula (validación combinada en anotaciones).
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    return value.matches(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{6,}$");
  }
}
