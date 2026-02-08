import {
  AbstractControl,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';

export function lengthValidator(
  minLength: number,
  maxLength: number,
): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value: number | string = control.value;
    if (
      value !== null &&
      (value.toString().length < minLength ||
        value.toString().length > maxLength)
    ) {
      return { invalidLength: { value: control.value } };
    }
    return null;
  };
}

export function noWhitespaceValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const isWhitespace = (control.value || '').trim().length === 0;
    const isValid = !isWhitespace;
    return isValid ? null : { whitespace: true };
  };
}

export function noSpecialCharactersValidator(): ValidatorFn {
  const specialCharacters = /[^a-zA-Z0-9]/;
  return (control: AbstractControl): ValidationErrors | null => {
    const forbidden: boolean = specialCharacters.test(control.value);
    return forbidden ? { forbiddenCharacters: { value: control.value } } : null;
  };
}

export function passwordStrengthValidator(): ValidatorFn {
  const strongPassword =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#])[A-Za-z\d@$!%*?&#]{6,}$/;
  return (control: AbstractControl): ValidationErrors | null => {
    const isValid: boolean = strongPassword.test(control.value);
    return isValid ? null : { weakPassword: { value: control.value } };
  };
}

/**
 * Preset de validadores para campos de texto: required + longitud + sin espacios en blanco.
 *
 * @param min - Longitud mínima permitida.
 * @param max - Longitud máxima permitida.
 * @returns Array de ValidatorFn para aplicar a un control de formulario.
 */
export function textFieldValidators(min: number, max: number): ValidatorFn[] {
  return [
    Validators.required,
    lengthValidator(min, max),
    noWhitespaceValidator(),
  ];
}

/**
 * Preset de validadores para campos numéricos como texto: required + longitud.
 *
 * @param min - Longitud mínima permitida (en dígitos).
 * @param max - Longitud máxima permitida (en dígitos).
 * @returns Array de ValidatorFn para aplicar a un control de formulario.
 */
export function numericFieldValidators(
  min: number,
  max: number,
): ValidatorFn[] {
  return [Validators.required, lengthValidator(min, max)];
}
