export interface AccessTokenPayload {
  aud: string;
  nbf: number;
  scope: string[];
  iss: string;
  exp: number;
  iat: number;
  jti: string;

  sub: string;
  username: string;
  rolesAndPermissions: string[];
  isAdmin: boolean;

  [key: string]: unknown;
}
