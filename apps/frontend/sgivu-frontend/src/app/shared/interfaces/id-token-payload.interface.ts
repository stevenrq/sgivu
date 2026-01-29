export interface IdTokenPayload {
  sub: string;
  aud: string;
  azp: string;
  auth_time: number;
  iss: string;
  exp: number;
  iat: number;
  nonce: string;
  jti: string;
  sid: string;

  userId: string;

  [key: string]: unknown;
}
