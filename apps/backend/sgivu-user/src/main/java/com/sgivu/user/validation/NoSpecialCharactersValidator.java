package com.sgivu.user.validation;

import com.sgivu.user.validation.annotation.NoSpecialCharacters;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoSpecialCharactersValidator
    implements ConstraintValidator<NoSpecialCharacters, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    return value.matches("^[a-zA-Z0-9]*$");
  }
}
