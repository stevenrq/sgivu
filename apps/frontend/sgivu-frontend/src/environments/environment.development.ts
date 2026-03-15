export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  // Preferido para desarrollo: usar el hostname del contenedor para que el comportamiento coincida con docker-compose y producción
  // Por defecto la SPA usará el hostname del Authorization Server `sgivu-auth`.
  // Si no puedes resolver `sgivu-auth` desde tu SO (p. ej. Windows sin entrada en hosts),
  // cámbialo temporalmente a 'http://localhost:9000' para acceder al servicio vía el loopback del host.
  issuer: 'http://sgivu-auth:9000',
  clientId: 'angular-local',
};
