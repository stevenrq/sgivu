# Instrucciones para agentes de documentación

## Sobre este proyecto

- Este sitio de documentación está construido con [Mintlify](https://mintlify.com).
- Las páginas están en formato MDX con frontmatter YAML.
- La navegación y estructura principal se configuran en docs.json.
- Usa mint dev para previsualización local.
- Usa mint broken-links para validar enlaces internos.

## Terminología

- SGIVU: nombre oficial de la plataforma.
- Gateway BFF: usar este término para la capa de Backend for Frontend.
- Auth Server: servicio de autorización OAuth2.1/OIDC (sgivu-auth).
- Discovery Server: registro Eureka (sgivu-discovery).
- Config Server: Spring Cloud Config (sgivu-config).
- Servicio ML: microservicio FastAPI para predicciones (sgivu-ml).
- Permisos: preferir "permisos" sobre "roles" cuando se hable de autorización fina.
- Base de datos por dominio: referirse a "múltiples bases de datos en un solo contenedor PostgreSQL" cuando aplique.

Términos técnicos que deben mantenerse en inglés por precisión:

- OAuth2, OIDC, JWT, endpoint, payload, token relay, circuit breaker, fallback, feature flag.

## Preferencias de estilo

- Escribir para humanos en español claro y directo.
- Mantener voz activa y frases cortas.
- Encabezados en formato oración.
- Texto visible al usuario siempre en español.
- Código, comandos, nombres de clases, propiedades y rutas técnicas en inglés.
- Evitar traducciones literales que degraden precisión técnica.
- Ser consistente con términos en toda la documentación.

## Límites de contenido

- Sí documentar:
  - Arquitectura, flujos funcionales, despliegue, operación y seguridad de SGIVU.
  - Uso de APIs y contratos funcionales orientados a consumidores internos.
  - Configuración por entorno y prácticas recomendadas de operación.
- No documentar:
  - Secretos reales, credenciales, tokens o valores sensibles.
  - Procedimientos internos no soportados oficialmente.
  - Detalles de implementación efímeros sin valor para usuarios del sistema.

## Reglas de migración EN a ES

- No renombrar archivos MDX ni rutas de navegación para evitar enlaces rotos.
- Traducir títulos, descripciones y contenido explicativo visible.
- Mantener intacta la sintaxis de componentes Mintlify (Card, Steps, Note, etc.).
- Verificar enlaces con mint broken-links antes de cerrar cambios.
