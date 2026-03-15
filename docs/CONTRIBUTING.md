# Contribuciones

Este documento consolida lineamientos ya definidos en el repositorio para contribuir en SGIVU.

## Flujo Base

1. Fork → branch → PR
2. Añadir tests para cambios funcionales

Variantes ya documentadas:

- Frontend: "Añadir tests para cambios funcionales y describir el cambio en el PR".
- ML: "Incluir tests para cambios funcionales (especialmente training/prediction)".

## Idioma

- **Código fuente** (clases, métodos, variables, archivos, logs, excepciones): **INGLÉS**.
- **Comentarios y documentación**: **ESPAÑOL**.
- **Textos visibles para el usuario** (UI, mensajes, validaciones, respuestas de error): **ESPAÑOL**.

Regla base:

> Lo que lee un humano → español.
> Lo que ejecuta la máquina → inglés.

## Pruebas

- `@DisplayName` (Spring / JUnit): **ESPAÑOL**.
- `describe()` e `it()` (Angular / Jasmine): **ESPAÑOL**.
- Nombres de métodos de test: **INGLÉS**.
- Patrón común en todos los stacks: **resultado esperado + condición**.

Reglas generales:

- Describir **comportamiento**, no implementación.
- Un test = una expectativa clara. Si el nombre tiene "and", probablemente son dos tests.
- Ser consistente en todo el proyecto: elegir un estilo y no mezclarlo.
- El nombre debe explicar el *por qué* del fallo: qué se rompió y en qué escenario.

## Calidad de Código

- Aplicar **SOLID**, **Clean Code** y **DRY**.
- Clases con una sola responsabilidad.
- Métodos pequeños y legibles.
- No usar valores mágicos ni lógica hardcodeada.

## Errores y Logs

- Mensajes de error al usuario: **español**.
- Logs y errores técnicos internos: **inglés**.
- No exponer detalles técnicos al usuario.

## Git

- Commits en **inglés**, siguiendo **Conventional Commits** y **Gitflow**.
- Commits pequeños y atómicos.
- Ramas con nombres claros (`main`, `develop`, `feature/`, `release/`, `hotfix/`, `fix/`, `refactor/`, `chore/`).

## Fuentes

- `README.md` (flujo base de contribuciones).
- `apps/frontend/sgivu-frontend/README.md` (variante frontend del flujo base).
- `apps/ml/sgivu-ml/README.md` (variante ML del flujo base).
- `.github/copilot-instructions.md` (idioma, pruebas, calidad y Git).
