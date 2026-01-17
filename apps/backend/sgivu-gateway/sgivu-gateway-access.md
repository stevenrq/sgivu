# Configuración de Acceso a sgivu-gateway

Este documento describe cómo acceder al servicio `sgivu-gateway` (BFF - Backend For Frontend) según
el entorno de despliegue.

## Resumen de Entornos

| Entorno | ¿Requiere `/etc/hosts`? | URL de acceso |
|---------|-------------------------|---------------|
| **Desarrollo local** | ✅ Solo `sgivu-auth` | `http://localhost:8080` |
| **Producción (EC2 + Nginx)** | ❌ No | `http://<ec2-hostname>` (puerto 80) |

---

## 1. Rol del Gateway como BFF

`sgivu-gateway` implementa el patrón **Backend For Frontend**:

- Actúa como cliente OAuth2 ante `sgivu-auth`
- Almacena tokens (`access_token`, `refresh_token`) en sesión Redis
- Expone `/auth/session` para que el frontend consulte el estado de autenticación
- Propaga el `access_token` a los microservicios downstream via `TokenRelay`

El navegador **nunca** maneja tokens directamente; solo mantiene una cookie de sesión (`SESSION`)
con el gateway.

---

## 2. Desarrollo Local (docker-compose.dev.yml)

En desarrollo local **no se usa Nginx**. Los puertos se exponen directamente:

- `sgivu-gateway` → puerto 8080 (accesible via `localhost:8080`)
- `sgivu-auth` → puerto 9000 (accesible via `sgivu-auth:9000`)

### Configuración requerida

Editar `/etc/hosts`:

```bash
sudo nano /etc/hosts
```

Agregar solo `sgivu-auth` (el gateway usa `localhost`):

```text
127.0.0.1 sgivu-auth
```

### Verificación

```bash
curl http://localhost:8080/actuator/health
```

### Flujo OAuth2 en desarrollo

1. Frontend Angular en `http://localhost:4200`
2. Login redirige a `http://sgivu-auth:9000/oauth2/authorize`
3. Callback llega a `http://localhost:8080/login/oauth2/code/sgivu-gateway`
4. Gateway almacena tokens en Redis y establece cookie `SESSION`

### Por qué solo se necesita mapear `sgivu-auth`

- El gateway está configurado con `SGIVU_GATEWAY_URL=http://localhost:8080` en `.env.dev`
- El auth server usa `SGIVU_AUTH_URL=http://sgivu-auth:9000`, por eso necesita el mapeo
- El navegador puede resolver `localhost` automáticamente, pero no `sgivu-auth`

---

## 3. Producción (EC2 + Nginx)

En producción se usa **Nginx como reverse proxy**. Todo el tráfico pasa por el puerto 80.

### Arquitectura

```
Frontend Angular (S3) → Nginx (puerto 80) → sgivu-gateway (8080 interno)
                                          → sgivu-auth (9000 interno)
```

### Configuración

**NO se requiere modificar `/etc/hosts`**.

El acceso se realiza usando el hostname público de EC2:

```bash
# Verificar sesión
curl http://ec2-XX-XX-XX-XX.compute-1.amazonaws.com/auth/session

# Health check
curl http://ec2-XX-XX-XX-XX.compute-1.amazonaws.com/actuator/health
```

### Rutas de Nginx relevantes

| Ruta | Destino |
|------|---------|
| `/auth/session` | `sgivu-gateway:8080` |
| `/oauth2/*` | `sgivu-auth:9000` |
| `/login`, `/logout` | `sgivu-auth:9000` |
| `/connect/logout` | `sgivu-auth:9000` |
| `/api/users/*`, `/api/clients/*`, etc. | `sgivu-gateway:8080` |

### Por qué funciona sin /etc/hosts

- Nginx unifica todo bajo un mismo dominio, eliminando problemas de cookies cross-origin
- El `ISSUER_URL` y `SGIVU_GATEWAY_URL` en `.env` usan el hostname público de EC2
- Los contenedores resuelven el hostname via `extra_hosts` en `docker-compose.yml`

---

## 4. Gestión de Sesiones

### Cookies

| Cookie | Servicio | Almacenamiento | Propósito |
|--------|----------|----------------|-----------|
| `SESSION` | Gateway | Redis | Sesión OAuth2 (tokens, state, PKCE) |
| `AUTH_SESSION` | Auth | PostgreSQL | Sesión del formulario de login |

Ambas cookies están configuradas con `SameSite=Lax` y `HttpOnly=true`.

### Endpoint de sesión

```bash
# Verificar si hay sesión activa
curl -c cookies.txt -b cookies.txt http://<host>/auth/session
```

Respuesta cuando hay sesión:

```json
{
  "username": "usuario@email.com",
  "roles": ["ROLE_USER"],
  "authenticated": true
}
```

---

## 5. Security Group (AWS)

Para producción, solo exponer el **puerto 80** (Nginx):

| Tipo | Protocolo | Puerto | Origen |
|------|-----------|--------|--------|
| HTTP | TCP | 80 | 0.0.0.0/0 |

**No es necesario** exponer el puerto 8080 externamente.

---

## 6. Resumen de Cambios entre Entornos

| Aspecto | Desarrollo Local | Producción (Nginx) |
|---------|------------------|-------------------|
| `/etc/hosts` | `127.0.0.1 sgivu-auth` | No requerido |
| URL del gateway | `http://localhost:8080` | `http://<ec2-hostname>` |
| Puerto expuesto | 8080 (directo) | 80 (Nginx) |
| Archivo compose | `docker-compose.dev.yml` | `docker-compose.yml` |
| Cookies cross-origin | Funciona con localhost | Nginx unifica dominio |
