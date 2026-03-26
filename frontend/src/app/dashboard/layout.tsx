'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import useSessionStore from '@/stores/useSession';
import { DashboardNavbar } from '@/components/dashboard';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isAuthenticated, accessToken } = useSessionStore();
  const router = useRouter();

  // Guard client-side: middleware já cobre o caso server-side via cookie
  useEffect(() => {
    if (!isAuthenticated || !accessToken) {
      router.push('/auth/login');
    }
  }, [isAuthenticated, accessToken, router]);

  if (!isAuthenticated || !accessToken) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-secondary-500">Redirecionando...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-canvas">
      <DashboardNavbar />
      <main>{children}</main>
    </div>
  );
}
