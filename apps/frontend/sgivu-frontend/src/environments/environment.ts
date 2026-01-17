export const environment = {
  production: true,
  // URL del Gateway a través de Nginx (puerto 80, sin especificar puerto)
  apiUrl: 'http://ec2-98-86-100-220.compute-1.amazonaws.com',
  // URL del Auth Server a través de Nginx (misma URL base, Nginx rutea internamente)
  issuer: 'http://ec2-98-86-100-220.compute-1.amazonaws.com',
  clientId: 'angular-client',
};
