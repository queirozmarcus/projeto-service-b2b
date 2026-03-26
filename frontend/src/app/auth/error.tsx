'use client';

import { useEffect } from 'react';
import { AuthCard } from '@/components/auth/AuthCard';
import { Button } from '@/components/ui/Button';

interface ErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function AuthError({ error, reset }: ErrorProps) {
  useEffect(() => {
    // Log error details in development
    if (process.env.NODE_ENV === 'development') {
      console.error('Auth error:', error);
    }
  }, [error]);

  return (
    <AuthCard title="Erro ao carregar página">
      <div className="space-y-4">
        <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800">
          <p className="font-semibold">Ocorreu um erro inesperado</p>
          <p className="mt-1 text-xs opacity-75">
            Tente novamente. Se o problema persistir, entre em contato com o suporte.
          </p>
        </div>
        <Button
          variant="outline"
          className="w-full"
          onClick={reset}
        >
          Tentar Novamente
        </Button>
      </div>
    </AuthCard>
  );
}
