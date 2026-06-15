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

## Documentación

Antes de mergear un PR que modifique funcionalidad, verificar:

- [ ] Si se modificaron endpoints: actualizar `docs/api/` correspondiente
- [ ] Si se modificaron configuraciones: actualizar `docs/config/` correspondiente
- [ ] Si se modificó la arquitectura: actualizar `docs/architecture.mdx` y diagramas
- [ ] Si se modificaron versiones/dependencias: actualizar tablas de versiones en `docs/services/`
- [ ] Ejecutar `mint broken-links` sin errores
- [ ] Todo el contenido nuevo en español (excepto código fuente)
- [ ] Comentarios en bloques de código en español

### Flujo de trabajo

1. **Rama:** `docs/<descripcion>` para cambios exclusivos de documentación
2. **Commits:** `docs: <descripcion>` (Conventional Commits)
3. **Preview:** `mint dev` para verificar cambios visualmente
4. **Validación:** `mint broken-links` antes de push

### Snippets reutilizables

Para información que se repite en múltiples páginas, usar snippets de `docs/snippets/`:

```mdx
<Snippet file="snippets/service-ports-table.mdx" />
```

Snippets disponibles:

- `service-ports-table.mdx` — Tabla de servicios y puertos
- `tech-stack.mdx` — Stack tecnológico y versiones
- `internal-auth-note.mdx` — Nota sobre autenticación interna entre servicios

## Fuentes

- `README.md` (flujo base de contribuciones).
- [README de sgivu-frontend](https://github.com/stevenrq/sgivu-frontend/blob/main/README.md) (variante frontend del flujo base; repo independiente).
- `apps/ml/sgivu-ml/README.md` (variante ML del flujo base).
- `.github/copilot-instructions.md` (idioma, pruebas, calidad y Git).
