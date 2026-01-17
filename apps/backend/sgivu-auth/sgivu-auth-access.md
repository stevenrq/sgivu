# Configuración de Acceso a sgivu-auth

Este documento describe cómo acceder al servicio `sgivu-auth` (Authorization Server) según el
entorno de despliegue.

## Resumen de Entornos

| Entorno | ¿Requiere `/etc/hosts`? | URL de acceso |
|---------|-------------------------|---------------|
| **Desarrollo local** | ✅ Sí | `http://sgivu-auth:9000` |
| **Producción (EC2 + Nginx)** | ❌ No | `http://<ec2-hostname>` (puerto 80) |

---

## 1. Desarrollo Local (docker-compose.dev.yml)

En desarrollo local **no se usa Nginx**. Los puertos de los servicios se exponen directamente:

- `sgivu-auth` → puerto 9000
- `sgivu-gateway` → puerto 8080

### Configuración requerida

Editar `/etc/hosts` para mapear el hostname del contenedor a localhost:

```bash
sudo nano /etc/hosts
```

Agregar:

```text
127.0.0.1 sgivu-auth
127.0.0.1 sgivu-gateway
```

### Verificación

```bash
curl http://sgivu-auth:9000/.well-known/openid-configuration
```

Respuesta esperada:

```json
{
    "issuer": "http://sgivu-auth:9000",
    "authorization_endpoint": "http://sgivu-auth:9000/oauth2/authorize",
    ...
}
```

### Por qué es necesario

- El navegador y el frontend Angular necesitan resolver `sgivu-auth` para acceder al Authorization
  Server.
- El `issuer` configurado en Spring Authorization Server usa `sgivu-auth:9000`, y debe coincidir
  con la URL que usa el navegador para evitar errores de validación de tokens.

---

## 2. Producción (EC2 + Nginx)

En producción se usa **Nginx como reverse proxy** en el puerto 80. Todo el tráfico pasa por el
hostname público de EC2.

### Arquitectura

```
Navegador → Nginx (puerto 80) → sgivu-auth (puerto 9000 interno)
                              → sgivu-gateway (puerto 8080 interno)
```

### Configuración

**NO se requiere modificar `/etc/hosts`** en la máquina del desarrollador.

El acceso se realiza directamente usando el hostname público de EC2:

```bash
curl http://ec2-XX-XX-XX-XX.compute-1.amazonaws.com/.well-known/openid-configuration
```

Respuesta esperada:

```json
{
    "issuer": "http://ec2-XX-XX-XX-XX.compute-1.amazonaws.com",
    "authorization_endpoint": "http://ec2-XX-XX-XX-XX.compute-1.amazonaws.com/oauth2/authorize",
    ...
}
```

### Por qué funciona sin /etc/hosts

- Nginx rutea las peticiones OAuth2 (`/oauth2/*`, `/login`, `/.well-known/*`) al contenedor
  `sgivu-auth:9000` internamente.
- El `ISSUER_URL` en `.env` está configurado con el hostname público de EC2.
- Los contenedores resuelven el hostname de EC2 via `extra_hosts` en `docker-compose.yml`.

---

## 3. Configuración del Security Group (AWS)

Para producción, solo es necesario exponer el **puerto 80** (Nginx) en el Security Group:

| Tipo | Protocolo | Puerto | Origen |
|------|-----------|--------|--------|
| HTTP | TCP | 80 | 0.0.0.0/0 (o tu IP) |

**No es necesario** exponer el puerto 9000 externamente; Nginx maneja el ruteo interno.

---

## 4. Resumen de Cambios entre Entornos

| Aspecto | Desarrollo Local | Producción (Nginx) |
|---------|------------------|-------------------|
| `/etc/hosts` | `127.0.0.1 sgivu-auth` | No requerido |
| URL del issuer | `http://sgivu-auth:9000` | `http://<ec2-hostname>` |
| Puerto expuesto | 9000 (directo) | 80 (Nginx) |
| Archivo compose | `docker-compose.dev.yml` | `docker-compose.yml` |
