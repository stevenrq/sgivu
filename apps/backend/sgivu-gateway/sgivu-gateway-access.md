# Configuracion de Acceso a sgivu-gateway en Entornos de Desarrollo

Este documento explica por que es necesario mapear el hostname `sgivu-gateway` en la maquina local
cuando el frontend y las herramientas externas acceden al gateway desde fuera de la red Docker.

## 1. Contexto General

`sgivu-gateway` actua como BFF (Backend For Frontend). El navegador inicia sesion contra
`sgivu-auth` y recibe el callback en el gateway. Para evitar que la SPA maneje tokens, el gateway
mantiene la sesión HTTP y expone `/auth/session`.

Dentro de la red Docker, los servicios se hablan usando el hostname del contenedor:

<http://sgivu-gateway:8080>

Fuera de Docker (navegador, Postman, frontend Angular), ese hostname no existe en DNS. Por eso
debemos mapearlo manualmente.

## 2. Mapeo de Hosts por Entorno

### Escenario A: Desarrollo Local

Cuando el gateway corre en Docker en la misma maquina, agrega este alias en `/etc/hosts`:

```text
127.0.0.1 sgivu-gateway
```

Esto permite que el navegador resuelva `sgivu-gateway` y acceda a `http://sgivu-gateway:8080`.

### Escenario B: Desarrollo Remoto (EC2)

Cuando los contenedores están en EC2, mapea el hostname al IP público:

```text
98.86.100.220 sgivu-gateway
```

Asegura el Security Group para permitir acceso al puerto 8080 solo desde tu IP.

## 3. Por Que el BFF Necesita Esto

- El gateway genera el `redirect_uri` para OAuth2 usando `gateway-client.url`.
- El navegador debe usar el mismo host en el login y el callback, porque Spring Authorization
  Server exige coincidencia exacta de `redirect_uri`.
- El gateway mantiene sesion con cookies. Si el frontend y el gateway no comparten el mismo host,
  el navegador puede bloquear cookies por politicas `SameSite`.

## 4. Por Que SGIVU_GATEWAY_URL Puede Ser localhost

`SGIVU_GATEWAY_URL` se usa en `sgivu-auth` para registrar el `redirect_uri` del cliente
`sgivu-gateway`. Ese valor debe coincidir con el host que usa el navegador. Por eso en desarrollo
puede ser `http://localhost:8080` si la SPA corre en `http://localhost:4200`.

Esto no afecta la comunicacion interna entre microservicios: ellos siguen resolviendose por
`sgivu-*` dentro de Docker y no necesitan conocer el hostname externo del gateway.

## 5. Verificacion Rapida

```bash
curl http://sgivu-gateway:8080/actuator/health
```

Si responde, el mapeo del host está correcto.

## 6. Recomendaciones

- Manten la misma base de host para frontend y gateway durante el desarrollo.
- En produccion, usa un dominio publico con HTTPS (ej. `api.sgivu.com`) y evita exponer puertos
  directamente.
