package com.sgivu.user.validation.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.TYPE_USE;

import com.sgivu.user.validation.PasswordStrengthValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = PasswordStrengthValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordStrength {
  String message() default
      "La contraseña debe contener al menos una letra minúscula, una letra mayúscula, un dígito, un"
          + " carácter especial y debe tener al menos 6 caracteres.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
