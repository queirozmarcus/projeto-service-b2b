'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import useSessionStore from '@/stores/useSession';

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isAuthenticated } = useSessionStore();
  const router = useRouter();

  // Redireciona usuário já autenticado para o dashboard
  useEffect(() => {
    if (isAuthenticated) {
      router.push('/dashboard');
    }
  }, [isAuthenticated, router]);

  return <>{children}</>;
}
