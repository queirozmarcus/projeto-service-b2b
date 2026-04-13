import { NextRequest, NextResponse } from 'next/server';

const PROTECTED_PREFIXES = ['/dashboard', '/proposals', '/workspaces'];
const AUTH_PREFIXES = ['/auth/login', '/auth/register'];

export function middleware(request: NextRequest) {
  const refreshToken = request.cookies.get('refreshToken');
  const { pathname } = request.nextUrl;

  const isProtected = PROTECTED_PREFIXES.some((p) => pathname.startsWith(p));
  const isAuthRoute = AUTH_PREFIXES.some((p) => pathname.startsWith(p));

  if (isProtected && !refreshToken) {
    return NextResponse.redirect(new URL('/auth/login', request.url));
  }

  if (isAuthRoute && refreshToken) {
    return NextResponse.redirect(new URL('/dashboard', request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico|api).*)'],
};
