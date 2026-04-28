'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import useSessionStore from '@/stores/useSession';
import { BuildingOffice2Icon } from '@heroicons/react/24/outline';

export function WorkspaceSetupBanner() {
  const { user, accessToken, setNeedsWorkspace } = useSessionStore();
  const [workspaceName, setWorkspaceName] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!workspaceName.trim()) {
      setError('Informe um nome para o workspace.');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      // 1. Create workspace
      const createRes = await fetch('/api/v1/workspaces', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        },
        body: JSON.stringify({ name: workspaceName.trim() }),
      });

      if (!createRes.ok) {
        const data = await createRes.json().catch(() => ({}));
        throw new Error(data?.detail ?? 'Erro ao criar workspace.');
      }

      // 2. Refresh token so JWT carries the new workspaceId
      const refreshRes = await fetch('/api/v1/auth/refresh', {
        method: 'POST',
        credentials: 'include',
      });

      if (!refreshRes.ok) {
        throw new Error('Erro ao atualizar sessão. Faça login novamente.');
      }

      // 3. Clear needsWorkspace flag and reload dashboard
      setNeedsWorkspace(false);
      window.location.reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro inesperado.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="rounded-xl border border-primary-200 bg-primary-50 px-6 py-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:gap-6">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary-100">
          <BuildingOffice2Icon className="h-5 w-5 text-primary-600" />
        </div>

        <div className="flex-1 space-y-4">
          <div>
            <h2 className="font-display text-lg font-black text-ink-900">
              Configure seu workspace
            </h2>
            <p className="mt-1 text-sm text-secondary-600">
              {user?.fullName
                ? `Olá, ${user.fullName.split(' ')[0]}! `
                : ''}
              Para começar a usar o ScopeFlow, crie seu workspace agora.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="flex flex-col gap-3 sm:flex-row sm:items-end">
            <div className="flex-1">
              <Input
                id="workspaceName"
                label="Nome do workspace"
                placeholder="Ex: Minha Agência"
                value={workspaceName}
                onChange={(e) => setWorkspaceName(e.target.value)}
                error={error ?? undefined}
                disabled={isLoading}
                autoFocus
              />
            </div>
            <Button
              type="submit"
              variant="primary"
              size="md"
              loading={isLoading}
              className="whitespace-nowrap"
            >
              Criar Workspace
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
