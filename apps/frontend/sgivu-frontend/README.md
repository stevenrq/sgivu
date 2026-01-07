# SGIVU - sgivu-frontend

## Descripción

Frontend Angular que provee vistas para gestión de usuarios, roles, clientes, vehículos y dashboard. Integra autenticación OAuth 2.0/OIDC y consume APIs vía SGIVU Gateway.

## Arquitectura y Rol

- Aplicación Angular standalone (sin NgModules) con rutas en `app.routes.ts` y configuración en `app.config.ts`.
- Features en `src/app/features` (auth, dashboard, users, clients, vehicles, purchase-sales) y `shared` para layout, directivas, servicios y validadores.
- Se integra con `sgivu-gateway` bajo el patrón BFF para la gestión segura de tokens.
- Configuración de entornos en `src/environments` (`environment.ts`, `environment.development.ts`).

## Tecnologías

- Angular 21, TypeScript, CSS3.

## Configuración

- Variables de entorno (archivos): `apiUrl` (gateway), `issuer` (auth), `clientId` (cliente OAuth), scopes si aplica.
- Copia base: `cp src/environments/environment.ts src/environments/environment.development.ts` y ajusta valores.

## Ejecución Local

```bash
npm install
npm start
# http://localhost:4200
```

Scripts útiles:

- `npm run build` (build prod en `dist/sgivu-frontend/browser`)
- `npm run watch` (recompila en cambios)
- `npm test` (pruebas unitarias)
- `npm run lint` (ESLint)
- `npx prettier --check "src/**/*.{ts,html,css}"`

## Endpoints Principales

- Base API: `apiUrl` apunta al gateway (por defecto `http://localhost:8080`).
- Issuer OIDC: `issuer` apunta a Auth (por defecto `http://localhost:9000`).
- Todo el tráfico HTTP pasa por SGIVU Gateway.

## Seguridad

- **Patrón BFF (Backend For Frontend):** `sgivu-gateway` actúa como BFF encargado de almacenar y servir el `access_token` y el `refresh_token` necesarios para la aplicación Angular. Aunque los tokens son creados por `sgivu-auth`, el gateway es el punto central de gestión para el frontend.
- `auth.guard` protege rutas; `auth.interceptor` adjunta `Authorization`.
- No hardcodear secretos; definirlos en `src/environments/*`.

## Dependencias

- Node.js 20+, npm 10+, Angular CLI opcional.
- Servicios SGIVU: gateway y auth para autenticación y APIs.

## Dockerización

- Imagen: `sgivu-frontend`.

Ejemplo:

```bash
docker build -t sgivu-frontend .
docker run -d -p 4200:80 -e API_URL=http://sgivu-gateway:8080 sgivu-frontend
```

## Build y Push Docker

- No hay script dedicado; usa `docker build` y publica la imagen en tu registry.

## Despliegue

- Contenedor Nginx sirviendo el build de Angular; configura `API_URL` según entorno.
- Exponer la app detrás de un Load Balancer o CDN y mantener `issuer` consistente con `sgivu-auth`.

## Monitoreo

- Logs del contenedor/Nginx y herramientas del navegador (Network/Console).

## Troubleshooting

- CORS bloqueado: ajusta `angular-client.url` en Config Server y `apiUrl` en el frontend.
- Login en loop: valida `issuer`, `clientId` y relojes sincronizados.
- 401/403: revisa scopes/roles en el JWT emitido por `sgivu-auth`.

## Buenas Prácticas y Convenciones

- TypeScript estricto, sin `any` salvo necesidad justificada.
- Componentes en PascalCase, archivos kebab-case; observables sufijo `$`, señales sufijo `Signal`.
- Evitar `ngClass/ngStyle` cuando un binding simple baste; usa `NgOptimizedImage` para estáticos.

## Diagramas

- Arquitectura general: `../../../docs/diagrams/01-system-architecture.puml`.

## Autor

- Steven Ricardo Quiñones (2025)
