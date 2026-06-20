# Configuracion de acceso a `sgivu-auth` (desarrollo y produccion)

Esta guia describe como acceder al Authorization Server en entorno local y en produccion,
priorizando la URL coherente con Docker (`http://sgivu-auth:9000`) y dejando `http://localhost:9000`
como alternativa temporal.

## Resumen rapido

- Recomendado: `http://sgivu-auth.127.0.0.1.nip.io:9000` (sin editar `hosts`).
- Alternativa: `http://sgivu-auth:9000` (requiere editar `hosts`).
- Fallback temporal: `http://localhost:9000`.
- Produccion: hostname publico de EC2 detras de Nginx (puerto 80).

## Matriz de decision

| Escenario | URL recomendada | Requiere hosts | Nota |
| --- | --- | --- | --- |
| Desarrollo local (recomendado) | `http://sgivu-auth.127.0.0.1.nip.io:9000` | No | nip.io resuelve a `127.0.0.1`; alias de red Docker para los contenedores |
| Desarrollo local (alias en hosts) | `http://sgivu-auth:9000` | Si | Coherencia con `issuer`; requiere `127.0.0.1 sgivu-auth` en `hosts` |
| Desarrollo local (fallback) | `http://localhost:9000` | No | Solo como fallback temporal; no llevar a produccion |
| Produccion (EC2 + Nginx) | `http://<ec2-hostname>` | No | Todo entra por Nginx en puerto 80 |

## 1. Desarrollo local recomendado: `http://sgivu-auth.127.0.0.1.nip.io:9000` (sin `hosts`)

Esta es la opcion recomendada porque evita que cada desarrollador edite el archivo `hosts`
de Windows o Linux, manteniendo a la vez la coherencia del `issuer`.

### 1.1 Como funciona

El issuer de OIDC debe ser la misma URL para quien emite el token (`sgivu-auth`), quien lo
valida (gateway y resource servers) y la URL por la que el navegador accede en el redirect
de login. El problema clasico es que el navegador (en Windows) no resuelve el nombre Docker
`sgivu-auth`. La solucion con nip.io cubre los dos lados a la vez:

- **Navegador / Postman / CLI:** `sgivu-auth.127.0.0.1.nip.io` se resuelve por DNS publico
  real a `127.0.0.1` -> puerto `9000` expuesto del contenedor. Sin tocar `hosts`.
- **Contenedores:** un alias de red en el servicio `sgivu-auth` (ver
  `infra/compose/sgivu-docker-compose/docker-compose.dev.yml`) hace que el DNS embebido de
  Docker resuelva ese mismo nombre al contenedor de auth, en lugar de caer al DNS publico
  (que dentro del contenedor apuntaria a si mismo).

### 1.2 Configuracion

Ya viene configurado en las plantillas. En `infra/compose/sgivu-docker-compose/.env.dev`:

```dotenv
ISSUER_URL=http://sgivu-auth.127.0.0.1.nip.io:9000
SGIVU_AUTH_URL=http://sgivu-auth.127.0.0.1.nip.io:9000
SGIVU_AUTH_DISCOVERY_URL=http://sgivu-auth.127.0.0.1.nip.io:9000/.well-known/openid-configuration
```

Y en el frontend [sgivu-frontend](https://github.com/stevenrq/sgivu-frontend),
`src/environments/environment.development.ts`:

```ts
issuer: 'http://sgivu-auth.127.0.0.1.nip.io:9000'
```

### 1.3 Verificacion

```bash
# Desde el host/WSL (simula el navegador de Windows via localhost-forwarding)
curl http://sgivu-auth.127.0.0.1.nip.io:9000/.well-known/openid-configuration

# Desde dentro de un contenedor (valida el alias de red Docker)
docker exec sgivu-gateway curl -s http://sgivu-auth.127.0.0.1.nip.io:9000/.well-known/openid-configuration
```

Respuesta esperada en ambos: JSON con `"issuer": "http://sgivu-auth.127.0.0.1.nip.io:9000"`.

### 1.4 Limitaciones

- Requiere acceso a internet/DNS (nip.io es un servicio publico). `sslip.io` es un
  equivalente si nip.io no responde.
- Algunas redes con proteccion de *DNS rebinding* bloquean nombres publicos que apuntan a
  `127.0.0.1`. En ese caso, usa el alias en `hosts` (seccion 2).

## 2. Alternativa con `hosts`: `http://sgivu-auth:9000`

Usa esta opcion si nip.io no resuelve en tu red. Requiere agregar el alias manualmente.

### 2.1 Requisito

El hostname `sgivu-auth` debe resolverse desde el sistema operativo donde se usa navegador,
Postman o CLI. Ademas, revertir las variables `ISSUER_URL` / `SGIVU_AUTH_URL` /
`SGIVU_AUTH_DISCOVERY_URL` a `http://sgivu-auth:9000` en `.env.dev`.

### 2.2 Configuracion de `hosts`

Linux:

```bash
sudo nano /etc/hosts
# agregar
127.0.0.1 sgivu-auth
```

Windows:

1. Abrir editor como Administrador.
2. Editar `C:\Windows\System32\drivers\etc\hosts`.
3. Agregar `127.0.0.1 sgivu-auth`.
4. Ejecutar en PowerShell (Admin):

```powershell
ipconfig /flushdns
```

### 2.3 Verificacion

```bash
curl http://sgivu-auth:9000/.well-known/openid-configuration
```

Respuesta esperada: JSON con `"issuer": "http://sgivu-auth:9000"`.

### 2.4 Notas importantes

- Configurar CORS en `sgivu-gateway` para incluir `http://localhost:4200`.

## 3. Fallback local: `http://localhost:9000`

Usar solo cuando no se desee modificar `hosts` o cuando `sgivu-auth` no resuelva en Windows/WSL.

### 3.1 Como aplicarlo

- Frontend (temporal): cambiar `issuer` a `http://localhost:9000` en
  `src/environments/environment.development.ts` (en el repo [sgivu-frontend](https://github.com/stevenrq/sgivu-frontend)),
  y alinear `ISSUER_URL` / `SGIVU_AUTH_URL` a `http://localhost:9000` en `.env.dev`.
- Postman / herramientas locales: usar `http://localhost:9000`.

### 3.2 Verificacion

```bash
curl http://localhost:9000/.well-known/openid-configuration
```

### 3.3 Advertencia

El `issuer` debe ser coherente en todos los servicios; no llevar esta excepcion de `localhost`
a produccion.

## 4. Recomendaciones practicas (WSL + Windows)

- Opcion recomendada: usa `sgivu-auth.127.0.0.1.nip.io` (seccion 1), sin tocar `hosts`.
- Si tu red bloquea nip.io, agrega `127.0.0.1 sgivu-auth` al `hosts` de Windows (seccion 2).
- Si necesitas una prueba rapida y aislada, usa `localhost` de forma temporal.
- Si aparecen errores CORS, valida que `sgivu-gateway` permita `http://localhost:4200`.

## 5. Checklist de diagnostico rapido

- `ERR_NAME_NOT_RESOLVED`: falta entrada en `hosts` o DNS cache sin limpiar.
- `issuer mismatch`: la URL del emisor no coincide entre cliente y servidor.
- Error CORS en SPA: origen del frontend no permitido por gateway.
- Sesion invalida en login OAuth2: revisar URL de `issuer` y redireccionamientos.

Comprobaciones utiles:

```bash
# Recomendado (nip.io, sin hosts), desde WSL/Linux o Windows
curl http://sgivu-auth.127.0.0.1.nip.io:9000/.well-known/openid-configuration

# Alternativa con hosts (si configuraste 127.0.0.1 sgivu-auth)
curl http://sgivu-auth:9000/.well-known/openid-configuration
```

En frontend, abrir `http://localhost:4200` y verificar en DevTools que no existan errores
`ERR_NAME_NOT_RESOLVED` ni bloqueos CORS en llamadas a `issuer` y `/auth/session`.

## 6. Produccion (EC2 + Nginx)

En produccion, Nginx es el punto de entrada (puerto 80) y enruta internamente hacia
`sgivu-auth` y `sgivu-gateway`.

### 6.1 Arquitectura

```text
Navegador -> Nginx (puerto 80) -> sgivu-auth (puerto 9000 interno)
                               -> sgivu-gateway (puerto 8080 interno)
```

### 6.2 Acceso

No se requiere modificar `/etc/hosts` en la maquina del desarrollador.

```bash
curl http://ec2-XX-XX-XX-XX.compute-1.amazonaws.com/.well-known/openid-configuration
```

Respuesta esperada (ejemplo):

```json
{
  "issuer": "http://ec2-XX-XX-XX-XX.compute-1.amazonaws.com",
  "authorization_endpoint": "http://ec2-XX-XX-XX-XX.compute-1.amazonaws.com/oauth2/authorize"
}
```

### 6.3 Por que funciona sin `hosts`

- Nginx enruta `/.well-known/*`, `/oauth2/*` y `/login` a `sgivu-auth`.
- `ISSUER_URL` apunta al hostname publico de EC2.
- Los contenedores resuelven ese hostname via `extra_hosts` en `docker-compose.yml`.

## 7. Security Group (AWS)

Exponer solo el puerto 80 para trafico HTTP publico:

| Tipo | Protocolo | Puerto | Origen |
| --- | --- | --- | --- |
| HTTP | TCP | 80 | `0.0.0.0/0` (o tu IP) |

No es necesario exponer el puerto 9000 externamente.

## 8. Resumen entre entornos

| Aspecto | Desarrollo local | Produccion |
| --- | --- | --- |
| `/etc/hosts` | No requerido (nip.io); `127.0.0.1 sgivu-auth` solo como alternativa | No requerido |
| URL `issuer` | `http://sgivu-auth.127.0.0.1.nip.io:9000` | `http://<ec2-hostname>` |
| Puerto publico | 9000 (directo) | 80 (Nginx) |
| Compose esperado | `docker-compose.dev.yml` | `docker-compose.yml` |
