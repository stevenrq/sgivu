# Guía del Repositorio

## Estructura del Proyecto y Módulos

- `src/app/`: configuración de arranque en `app.config.ts` y ruteo en `app.routes.ts`.
- `src/app/features`: `auth` (guards/interceptor/login OAuth), `dashboard`, `users` (CRUD, formularios, roles), `clients`, `vehicles`, `purchase-sales`.
- `src/app/shared`: componentes de layout, directivas (`appHasPermission`), servicios, validadores, modelos/interfaces.
- `src/environments`: configuraciones (`environment.ts`, `environment.development.ts`) consumidas por auth y servicios de API; mantén ambos archivos alineados.
- `src/assets` para estáticos; `../../../docs/diagrams/*.puml` para diagramas; pruebas junto al código como `*.spec.ts`.

## Comandos de Build, Pruebas y Desarrollo

- `npm start`: servidor de desarrollo en `http://localhost:4200` con recarga en vivo.
- `npm run build`: build de producción en `dist/sgivu-frontend/browser`.
- `npm run watch`: recompila al cambiar archivos.
- `npm test`: pruebas unitarias con Jasmine + Karma.
- `npm run lint`: ESLint vía `angular-eslint`.
- `npx prettier --check "src/**/*.{ts,html,css}"`: verificación de formato.

## Estilo de Código y Convenciones

- TypeScript estricto; prioriza inferencia y evita `any`; servicios/clases en `PascalCase`, archivos en `kebab-case`, observables con sufijo `$`, señales con sufijo `Signal`.
- Componentes standalone por defecto, `ChangeDetectionStrategy.OnPush`, estado con signals/computed; evita NgModules.
- Plantillas: usa control de flujo nativo `@if`/`@for`, sin funciones flecha; evita `ngClass`/`ngStyle` en favor de bindings de clase/estilo; usa `NgOptimizedImage` para imágenes estáticas.
- Enlaza host en el objeto `host` de los metadatos; indentación de 2 espacios; deja que ESLint + Prettier definan el formato.

## Guía de Pruebas

- Usa Jasmine/Karma con `*.spec.ts` colocados junto a la unidad bajo prueba.
- Cubre guards, interceptores, servicios con HTTP simulado y señales/salidas de componentes; valida la protección de rutas y la directiva de permisos.
- Ejecuta `npm test` antes de PR; `ng test --code-coverage` para revisar cobertura en flujos críticos.

## Commits y Pull Requests

- Adopta Conventional Commits (`feat:`, `fix:`, `chore:`…) y mantén el alcance acotado.
- PRs: resumen breve, issue/tarea vinculada, capturas/GIFs para cambios de UI, notas de entornos tocados y comandos de prueba ejecutados.
- Asegura que `npm run lint` y `npm test` pasen; documenta cualquier verificación omitida intencionalmente.

## Seguridad y Configuración

- Define `apiUrl`, `issuer` y `clientId` solo en `src/environments/*`; no hardcodees endpoints ni secretos en servicios/componentes.
- `angular-oauth2-oidc` maneja los tokens; evita registrar encabezados sensibles o guardar tokens fuera del storage OAuth provisto.
- Envía nuevas llamadas API a los dominios del gateway indicados por los entornos y mantén los orígenes CORS alineados con el host del frontend.

## Notas Específicas del Servicio

- Alinea `environment.ts` y `environment.development.ts` cuando cambien endpoints o scopes OAuth.
