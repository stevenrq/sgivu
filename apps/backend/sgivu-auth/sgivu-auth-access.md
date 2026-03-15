# Configuracion de acceso a `sgivu-auth` (desarrollo y produccion)

Esta guia describe como acceder al Authorization Server en entorno local y en produccion,
priorizando la URL coherente con Docker (`http://sgivu-auth:9000`) y dejando `http://localhost:9000`
como alternativa temporal.

## Resumen rapido

- Recomendado: `http://sgivu-auth:9000`.
- Alternativa temporal: `http://localhost:9000`.
- Produccion: hostname publico de EC2 detras de Nginx (puerto 80).

## Matriz de decision

| Escenario | URL recomendada | Requiere hosts | Nota |
| --- | --- | --- | --- |
| Desarrollo local (flujo normal) | `http://sgivu-auth:9000` | Si | Mantiene coherencia con `issuer` en servicios y Docker |
| Desarrollo local (sin tocar hosts) | `http://localhost:9000` | No | Solo como fallback temporal |
| Produccion (EC2 + Nginx) | `http://<ec2-hostname>` | No | Todo entra por Nginx en puerto 80 |

## 1. Desarrollo local recomendado: `http://sgivu-auth:9000`

Usa esta opcion para evitar discrepancias de `issuer` entre frontend, gateway y microservicios.

### 1.1 Requisito

El hostname `sgivu-auth` debe resolverse desde el sistema operativo donde se usa navegador,
Postman o CLI.

### 1.2 Configuracion de `hosts`

Linux / WSL2:

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

### 1.3 Verificacion

```bash
curl http://sgivu-auth:9000/.well-known/openid-configuration
```

Respuesta esperada: JSON con `"issuer": "http://sgivu-auth:9000"`.

### 1.4 Notas importantes

- Mantener `apps/frontend/sgivu-frontend/src/environments/environment.development.ts` con
  `issuer: 'http://sgivu-auth:9000'` cuando sea posible.
- Configurar CORS en `sgivu-gateway` para incluir `http://localhost:4200`.

## 2. Fallback local: `http://localhost:9000`

Usar solo cuando no se desee modificar `hosts` o cuando `sgivu-auth` no resuelva en Windows/WSL2.

### 2.1 Como aplicarlo

- Frontend (temporal): cambiar `issuer` a `http://localhost:9000` en
  `apps/frontend/sgivu-frontend/src/environments/environment.development.ts`.
- Postman / herramientas locales: usar `http://localhost:9000`.

### 2.2 Verificacion

```bash
curl http://localhost:9000/.well-known/openid-configuration
```

### 2.3 Advertencia

Si los servicios usan `issuer = http://sgivu-auth:9000`, no llevar esta excepcion de `localhost`
a produccion.

## 3. Recomendaciones practicas (WSL2 + Windows)

- Si el navegador corre en Windows, agrega `sgivu-auth` al `hosts` de Windows.
- Si necesitas una prueba rapida y aislada, usa `localhost` de forma temporal.
- Si aparecen errores CORS, valida que `sgivu-gateway` permita `http://localhost:4200`.

## 4. Checklist de diagnostico rapido

- `ERR_NAME_NOT_RESOLVED`: falta entrada en `hosts` o DNS cache sin limpiar.
- `issuer mismatch`: la URL del emisor no coincide entre cliente y servidor.
- Error CORS en SPA: origen del frontend no permitido por gateway.
- Sesion invalida en login OAuth2: revisar URL de `issuer` y redireccionamientos.

Comprobaciones utiles:

```bash
# Desde WSL/Linux
curl http://localhost:9000/.well-known/openid-configuration

# Desde Windows (si configuraste hosts)
curl http://sgivu-auth:9000/.well-known/openid-configuration
```

En frontend, abrir `http://localhost:4200` y verificar en DevTools que no existan errores
`ERR_NAME_NOT_RESOLVED` ni bloqueos CORS en llamadas a `issuer` y `/auth/session`.

## 5. Produccion (EC2 + Nginx)

En produccion, Nginx es el punto de entrada (puerto 80) y enruta internamente hacia
`sgivu-auth` y `sgivu-gateway`.

### 5.1 Arquitectura

```text
Navegador -> Nginx (puerto 80) -> sgivu-auth (puerto 9000 interno)
                               -> sgivu-gateway (puerto 8080 interno)
```

### 5.2 Acceso

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

### 5.3 Por que funciona sin `hosts`

- Nginx enruta `/.well-known/*`, `/oauth2/*` y `/login` a `sgivu-auth`.
- `ISSUER_URL` apunta al hostname publico de EC2.
- Los contenedores resuelven ese hostname via `extra_hosts` en `docker-compose.yml`.

## 6. Security Group (AWS)

Exponer solo el puerto 80 para trafico HTTP publico:

| Tipo | Protocolo | Puerto | Origen |
| --- | --- | --- | --- |
| HTTP | TCP | 80 | `0.0.0.0/0` (o tu IP) |

No es necesario exponer el puerto 9000 externamente.

## 7. Resumen entre entornos

| Aspecto | Desarrollo local | Produccion |
| --- | --- | --- |
| `/etc/hosts` | `127.0.0.1 sgivu-auth` (recomendado) | No requerido |
| URL `issuer` | `http://sgivu-auth:9000` | `http://<ec2-hostname>` |
| Puerto publico | 9000 (directo) | 80 (Nginx) |
| Compose esperado | `docker-compose.dev.yml` | `docker-compose.yml` |
