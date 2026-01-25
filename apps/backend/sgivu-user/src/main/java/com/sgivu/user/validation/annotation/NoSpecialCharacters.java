package com.sgivu.user.validation.annotation;

import static java.lang.annotation.ElementType.*;

import com.sgivu.user.validation.NoSpecialCharactersValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = NoSpecialCharactersValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoSpecialCharacters {
  String message() default "No se permiten caracteres especiales";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
