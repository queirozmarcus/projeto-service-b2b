import { jwtDecode } from 'jwt-decode';

export interface DecodedToken {
  sub: string;
  email: string;
  role: string;
  workspaceId: string;
  iat: number;
  exp: number;
}

export function decodeToken(token: string): DecodedToken | null {
  try {
    return jwtDecode<DecodedToken>(token);
  } catch {
    return null;
  }
}

export function isTokenExpired(token: string): boolean {
  const decoded = decodeToken(token);
  if (!decoded) return true;
  return Date.now() >= decoded.exp * 1000;
}

export function getTokenExpiresIn(token: string): number {
  const decoded = decodeToken(token);
  if (!decoded) return 0;
  return decoded.exp * 1000 - Date.now();
}

/** Retorna true se o token expira em menos de 60 segundos */
export function shouldRefreshToken(token: string): boolean {
  const expiresIn = getTokenExpiresIn(token);
  return expiresIn < 60_000;
}
