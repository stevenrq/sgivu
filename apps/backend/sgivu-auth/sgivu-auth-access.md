# Configuración de Acceso a sgivu-auth en Entornos de Desarrollo

Este documento consolida las guías para configurar el acceso al servicio sgivu-auth (Authorization Server) desde herramientas externas a la red de Docker, cubriendo tanto un entorno de desarrollo local como uno remoto desplegado en AWS EC2.

## 1. Contexto General y Problema Común

El proyecto utiliza microservicios basados en Spring Cloud, donde sgivu-auth actúa como Authorization Server. Este servicio se ejecuta en un contenedor de Docker y expone el puerto 9000.

Dentro de la red interna de Docker, los microservicios se comunican entre sí utilizando el nombre del contenedor como hostname:

<http://sgivu-auth:9000>

El problema principal es que las herramientas externas a esta red de Docker (como Postman, un frontend en Angular, o cualquier cliente en la máquina del desarrollador) no pueden resolver el nombre de host sgivu-auth, ya que no es un dominio registrado en un DNS público. Esto impide acceder a endpoints críticos para el flujo de autenticación, como:

<http://sgivu-auth:9000/.well-known/openid-configuration>

## 2. Soluciones según el Entorno de Desarrollo

La solución consiste en mapear el hostname sgivu-auth a la dirección IP correcta editando el archivo /etc/hosts en la máquina del desarrollador. La IP a utilizar dependerá de dónde se esté ejecutando el contenedor.

### Escenario A: Entorno de Desarrollo Local

En este escenario, el contenedor de sgivu-auth se ejecuta en la misma máquina local que las herramientas de desarrollo.

Solución Implementada

Se debe modificar el archivo /etc/hosts de la máquina local (host) para que el nombre sgivu-auth apunte a localhost.

Editar el archivo /etc/hosts:

En macOS/Linux: sudo nano /etc/hosts

En Windows: C:\Windows\System32\drivers\etc\hosts (ejecutar como administrador)

Agregar la siguiente línea:

```text
127.0.0.1 sgivu-auth
```

127.0.0.1: Es la dirección de localhost, es decir, la propia máquina.

sgivu-auth: Es el alias que ahora resolverá a 127.0.0.1.

Cualquier solicitud a <http://sgivu-auth:9000> desde la máquina host será redirigida a 127.0.0.1:9000, que es donde Docker expone el puerto del contenedor.

### Escenario B: Entorno de Desarrollo Remoto (AWS EC2)

En este escenario, los contenedores están desplegados en una instancia de AWS EC2, y se necesita acceso desde una máquina local externa.

Solución Implementada

La solución requiere dos pasos: mapear el hostname a la IP pública de EC2 y configurar el firewall de AWS (Security Group) para permitir el acceso.

Editar el archivo /etc/hosts (en la máquina local):
Agrega una línea que mapee el hostname sgivu-auth a la IP pública de tu instancia EC2.

```text
<EC2_PUBLIC_IP> sgivu-auth
```

Ejemplo:

```text
3.90.210.123 sgivu-auth
```

Configurar el Security Group (SG) en AWS EC2:
Se debe permitir el tráfico entrante al puerto 9000 desde tu IP pública local para mantener la seguridad.

Tipo: Custom TCP

Protocolo: TCP

Rango de puertos: 9000

Origen: My IP (o introduce tu IP pública seguida de /32, ej: 190.123.45.67/32)

¡Advertencia de Seguridad! Evita configurar el origen como 0.0.0.0/0 (Anywhere), ya que expondría el servicio a todo internet. Limita siempre el acceso a IPs específicas.

## 3. Verificación del Servicio

Una vez aplicada la configuración para cualquiera de los dos escenarios, puedes verificar el acceso desde tu máquina local ejecutando el siguiente comando en la terminal:

````bash
curl http://sgivu-auth:9000/.well-known/openid-configuration
````

El servicio debería responder con la configuración OIDC completa en formato JSON, confirmando que la conexión fue exitosa.

## 4. Conclusiones y Recomendaciones Futuras

Coherencia: Esta técnica permite que tanto los microservicios internos como las herramientas externas utilicen la misma URL (<http://sgivu-auth:9000>), simplificando la configuración y las pruebas.

Uso en Desarrollo: La modificación del archivo /etc/hosts es una solución práctica y efectiva exclusivamente para entornos de desarrollo.

Recomendación para Producción: En un entorno de producción, la resolución de nombres debe gestionarse a través de un DNS con un dominio público (ej. auth.sgivu.com). El servicio debe exponerse de forma segura a través de un proxy inverso (Nginx, Traefik) o un API Gateway, utilizando siempre HTTPS con certificados SSL/TLS válidos.

Buenas Prácticas: Se recomienda mantener configuraciones separadas para los diferentes entornos (desarrollo, producción) utilizando herramientas como Spring Cloud Config para evitar "hardcodear" URLs o valores sensibles en el código.
