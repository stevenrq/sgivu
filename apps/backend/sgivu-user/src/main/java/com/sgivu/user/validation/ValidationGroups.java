package com.sgivu.user.validation;

/**
 * Grupos de validación para aplicar reglas distintas entre creación y actualización sin duplicar
 * DTOs o entidades. Se usan con {@link org.springframework.validation.annotation.Validated} para
 * marcar campos obligatorios en altas y opcionales o inmutables en modificaciones.
 */
public interface ValidationGroups {

  /** Reglas que aplican al crear recursos. */
  interface Create {}

  /** Reglas que aplican al actualizar recursos existentes. */
  interface Update {}
}
