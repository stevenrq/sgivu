# Configuración de acceso a `sgivu-auth` (desarrollo — WSL2 y Windows)

Este documento explica cómo usar **ambas opciones** para acceder al Authorization Server en
entorno local: **priorizando `http://sgivu-auth:9000`** (recomendado para coherencia con Docker) y
ofreciendo la **alternativa `http://localhost:9000`** para equipos Windows o cuando no se quiera
modificar el hosts.

## Resumen rápido

- Recomendado (prioridad): `http://sgivu-auth:9000` — coincide con `docker-compose` y con la
  configuración del `issuer` en los servicios.
- Alternativa (fallback): `http://localhost:9000` — útil en Windows/WSL2 o Postman sin entradas en
  `hosts`.

## 1. Uso preferente — `http://sgivu-auth:9000` (coherente con Docker)

Por qué: el `issuer` configurado por defecto en los servicios y en la mayoría de los entornos de
composición es `http://sgivu-auth:9000`; usar el mismo hostname en el navegador evita discrepancias
entre entornos.

Requisitos (desarrollo local):

- `sgivu-auth` debe resolverse desde tu OS (añadir entrada en `hosts` si es necesario).

Linux / WSL2 (ejemplo):

```bash
sudo nano /etc/hosts
# añadir
127.0.0.1 sgivu-auth
```

Windows (si usas navegador/Postman en Windows):

1. Abrir editor como Administrador y editar `C:\Windows\System32\drivers\etc\hosts`.
2. Añadir la línea:

```text
127.0.0.1 sgivu-auth
```

1. Ejecutar en PowerShell (Admin):

```powershell
ipconfig /flushdns
```

Verificación (desde la máquina donde abres el navegador / Postman):

```bash
curl http://sgivu-auth:9000/.well-known/openid-configuration
# debe responder JSON con "issuer": "http://sgivu-auth:9000"
```

Notas:

- Mantén `environment.development.ts` apuntando a `http://sgivu-auth:9000` para que el SPA se
  comporte igual que en Docker/producción.
- `allowedOrigins` / CORS en `sgivu-gateway` debe incluir `http://localhost:4200` (frontend) —
  esto es independiente del hostname del Authorization Server.

## Alternativa / fallback — `http://localhost:9000` (cuando no quieres editar hosts)

Cuándo usar: pruebas rápidas en Windows/WSL2 o con Postman cuando `sgivu-auth` no se resuelve.

Cómo usar:

- Temporalmente en el frontend: editar `apps/frontend/sgivu-frontend/src/environments/environment.development.ts`
  y cambiar `issuer` a `http://localhost:9000` (solo para desarrollo).
- En Postman / herramientas: usar `http://localhost:9000` en las variables de entorno.

Verificación:

```bash
curl http://localhost:9000/.well-known/openid-configuration
```

Advertencia: si tu `issuer` en los servicios está configurado como `http://sgivu-auth:9000`, usar
`localhost` en la SPA es una excepción únicamente para desarrollo — no aplicar en producción.

## Recomendaciones prácticas (WSL2 + Windows)

- Preferencia: configura `sgivu-auth` en el `hosts` de Windows si trabajas con el navegador en
  Windows; así la URL oficial `http://sgivu-auth:9000` funciona en ambos lados (WSL y Windows).
- Si necesitas no tocar `hosts`, usa `http://localhost:9000` en la SPA temporalmente.
- Asegúrate de que `sgivu-gateway` permite `http://localhost:4200` como origen para evitar
  errores CORS.

## Comprobaciones útiles

- Desde WSL: `curl http://localhost:9000/.well-known/openid-configuration`
- Desde Windows (PowerShell): `curl http://sgivu-auth:9000/.well-known/openid-configuration`
  después de añadir la entrada en hosts
- Frontend: abrir `http://localhost:4200` y verificar en DevTools que las llamadas al issuer y a
  `/auth/session` no devuelvan ERR_NAME_NOT_RESOLVED ni errores CORS.

## 2. Producción (EC2 + Nginx)

En producción se usa **Nginx como reverse proxy** en el puerto 80. Todo el tráfico pasa por el
hostname público de EC2.

### Arquitectura

```text
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

| Tipo | Protocolo | Puerto | Origen              |
|------|-----------|--------|---------------------|
| HTTP | TCP       | 80     | 0.0.0.0/0 (o tu IP) |

**No es necesario** exponer el puerto 9000 externamente; Nginx maneja el ruteo interno.

---

## 4. Resumen de Cambios entre Entornos

| Aspecto | Desarrollo Local | Producción (Nginx) |
| --------- | ------------------- | ------------------- |
| `/etc/hosts` | `127.0.0.1 sgivu-auth` | No requerido |
| URL del issuer | `http://sgivu-auth:9000` | `http://<ec2-hostname>` |
| Puerto expuesto | 9000 (directo) | 80 (Nginx) |
| Archivo compose | `docker-compose.dev.yml` | `docker-compose.yml` |
