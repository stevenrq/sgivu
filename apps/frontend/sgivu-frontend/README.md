# sgivu-frontend - SGIVU

## Descripción

**`sgivu-frontend`** es la aplicación Single Page (SPA) cliente del ecosistema **SGIVU**, implementada en Angular. Proporciona la interfaz de usuario para administración (dashboards, gestión de clientes, usuarios, vehículos y contratos de compra/venta) y delega la autenticación y APIs al gateway (`sgivu-gateway`).

## Tecnologías y Dependencias

- Angular 21
- TypeScript
- Bootstrap 5 + Bootstrap Icons
- Chart.js + ng2-charts
- RxJS
- Herramientas de desarrollo: `@angular/cli`, `karma`/`jasmine` (tests), `eslint`, `prettier`

## Requisitos Previos

- Node.js (versión compatible con Angular 21)
- npm 8+ (se usa `package-lock.json`)
- `sgivu-config`, `sgivu-discovery`, `sgivu-gateway` y `sgivu-auth` disponibles (o arrancados via `infra/compose/sgivu-docker-compose`)

## Arranque y Ejecución

### Desarrollo

1. Instalar dependencias y arrancar el servidor de desarrollo:

   `npm install`
   `npm run start`  (dev server - por defecto en el puerto 4200)

2. Durante el desarrollo la configuración de entorno usada por defecto es `src/environments/environment.development.ts`.

### Ejecución Local (build)

`npm run build` — genera los assets en `dist/sgivu-frontend`.

## Despliegue

- Recomendación de despliegue: compilar la SPA (`npm run build`) y servir los archivos estáticos desde S3 + CloudFront (o un Nginx) como se hace en `infra/nginx` (actualmente la infra apunta a `sgivu-frontend.s3-website-us-east-1.amazonaws.com`).

## Producción

- Build optimizado: `npm run build -- --configuration production` (o usar la configuración por defecto para producción del builder de Angular).
- Servir `dist/sgivu-frontend` desde un CDN o un servidor estático (S3 + CloudFront es la opción utilizada por la infraestructura). Asegurar que `base href` y `routing` funcionan correctamente detrás del proxy.

## Endpoints / Integraciones

- BFF / Gateway: la app comunica con el backend a través de la URL configurada en `environment.apiUrl` (el gateway expone `/auth/session`, `/oauth2/authorization/sgivu-gateway` y proxifica las APIs `/v1/*`).
- Autenticación: el flujo de login se delega al gateway (OAuth2/OIDC, flow PKCE manejado por el gateway). La app consulta `/auth/session` para comprobar la sesión.

## Seguridad

- La aplicación no maneja directamente secretos; la autenticación es delegada a `sgivu-gateway` / `sgivu-auth`.
- Variables de entorno leídas desde `src/environments/*.ts`: `apiUrl`, `issuer`, `clientId` (configuración por entorno). No incluir valores secretos en el repositorio.

## Observabilidad

- El servicio BFF (`sgivu-gateway`) proporciona trazabilidad y endpoints que la UI consume (p. ej. `/auth/session`).

## Pruebas

- Unit tests: `npm run test` (Karma + Jasmine)
- Lint: `npm run lint`
- No hay tests E2E (Cypress/Playwright) configurados por defecto.

## Solución de Problemas

- Problema: 401/403 en peticiones XHR -> Verificar que `apiUrl` apunta al `sgivu-gateway` correcto y que la sesión está creada (`/auth/session`).
- Problema: Issuer / redirect mismatch -> revisar configuración de `issuer` y la configuración de `ISSUER_URL` en `sgivu-auth` / Nginx.
- Problema: Rutas no encontradas tras deploy estático -> comprobar `base href` en `index.html` y reglas de reescritura del servidor (serve index.html para rutas SPA).

## Contribuciones

1. Fork → branch → PR
2. Añadir tests para cambios funcionales y describir el cambio en el PR
