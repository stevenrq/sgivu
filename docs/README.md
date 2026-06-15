# Documentación de SGIVU

Este directorio contiene la documentación oficial del proyecto SGIVU construida con Mintlify.

## Contenido de la documentación

En este sitio encontrarás:

- Guías de instalación y puesta en marcha
- Arquitectura de microservicios
- Funcionalidades principales de negocio
- Referencia de APIs
- Infraestructura, seguridad y operación

## Escritura asistida por IA

Si usas herramientas de IA para redactar o mantener documentación en Mintlify, instala el skill oficial:

```bash
npx skills add https://mintlify.com/docs
```

Este comando agrega referencias de componentes, estándares editoriales y flujos recomendados para documentación en Mintlify.

## Desarrollo local

Instala el CLI de Mintlify:

```bash
npm i -g mint
```

Desde esta carpeta (donde está el archivo docs.json), inicia el preview local:

```bash
mint dev
```

El sitio quedará disponible en <http://localhost:3000>.

## Validación antes de publicar

Verifica enlaces rotos:

```bash
mint broken-links
```

Genera build estático:

```bash
mint build
```

## Publicación

Los cambios se publican automáticamente al enviar commits a la rama principal configurada para despliegue. Si aplica en tu organización, integra el GitHub App desde el panel de Mintlify.

## Solución de problemas

- Si el entorno local no levanta: ejecuta `mint update` para actualizar el CLI.
- Si una página devuelve 404: valida que estés en la carpeta correcta y que exista un docs.json válido.

## Recursos

- [Documentación de Mintlify](https://mintlify.com/docs)
- [Repositorio SGIVU](https://github.com/stevenrq/sgivu)
