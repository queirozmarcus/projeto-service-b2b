'use client';

import useSessionStore from '@/stores/useSession';

export default function DashboardPage() {
  const { user } = useSessionStore();

  return (
    <div className="mx-auto max-w-7xl px-6 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-secondary-900">Dashboard</h1>
        <p className="mt-1 text-secondary-600">
          Bem-vindo{user ? `, ${user.fullName}` : ''}! Gerencie suas propostas
          aqui.
        </p>
      </div>

      <div className="grid gap-6 md:grid-cols-3">
        <div className="rounded-xl border border-secondary-200 bg-white p-6">
          <h2 className="text-lg font-semibold text-secondary-900">
            Propostas
          </h2>
          <p className="mt-1 text-sm text-secondary-600">
            Nenhuma proposta criada ainda.
          </p>
        </div>
        <div className="rounded-xl border border-secondary-200 bg-white p-6">
          <h2 className="text-lg font-semibold text-secondary-900">
            Briefings
          </h2>
          <p className="mt-1 text-sm text-secondary-600">
            Nenhum briefing em andamento.
          </p>
        </div>
        <div className="rounded-xl border border-secondary-200 bg-white p-6">
          <h2 className="text-lg font-semibold text-secondary-900">
            Aprovações
          </h2>
          <p className="mt-1 text-sm text-secondary-600">
            Nenhuma aprovação pendente.
          </p>
        </div>
      </div>
    </div>
  );
}
