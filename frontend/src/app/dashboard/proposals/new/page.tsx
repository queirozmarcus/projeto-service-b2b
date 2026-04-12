'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ChevronLeftIcon, DocumentPlusIcon } from '@heroicons/react/24/outline';
import useDashboardStore from '@/stores/useDashboardStore';
import type { Proposal } from '@/components/dashboard/ProposalList';

const SERVICE_TYPE_OPTIONS: { value: string; label: string }[] = [
  { value: 'SOCIAL_MEDIA', label: 'Social Media Management' },
  { value: 'LANDING_PAGE', label: 'Landing Page Design' },
  { value: 'WEB_DESIGN', label: 'Web Design' },
  { value: 'BRANDING', label: 'Branding' },
  { value: 'VIDEO_PRODUCTION', label: 'Video Production' },
  { value: 'CONSULTING', label: 'Consulting' },
];

export default function NewProposalPage() {
  const router = useRouter();

  const [clientName, setClientName] = useState('');
  const [serviceType, setServiceType] = useState('');
  const [description, setDescription] = useState('');
  const [errors, setErrors] = useState<{ clientName?: string; serviceType?: string }>({});

  function validate(): boolean {
    const next: typeof errors = {};
    if (!clientName.trim()) next.clientName = 'Nome do cliente é obrigatório.';
    if (!serviceType) next.serviceType = 'Selecione um tipo de serviço.';
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!validate()) return;

    const proposal: Proposal = {
      id: crypto.randomUUID(),
      clientName: clientName.trim(),
      serviceType,
      status: 'DRAFT',
      createdAt: new Date().toISOString(),
    };

    useDashboardStore.getState().addProposal(proposal);
    router.push('/dashboard');
  }

  return (
    <div className="min-h-screen bg-canvas">
      <div className="mx-auto max-w-7xl px-6 py-8 space-y-8">

        {/* ── Breadcrumb ───────────────────────────────────────────── */}
        <div className="flex items-center gap-2 text-sm text-secondary-500">
          <Link
            href="/dashboard"
            className="inline-flex items-center gap-1 font-medium hover:text-ink-900 transition-colors"
          >
            <ChevronLeftIcon className="h-4 w-4" />
            Dashboard
          </Link>
          <span>/</span>
          <span className="text-ink-900 font-semibold">Nova Proposta</span>
        </div>

        {/* ── Page Header ──────────────────────────────────────────── */}
        <div className="flex items-center gap-3">
          <DocumentPlusIcon className="h-7 w-7 text-primary-500" />
          <h1 className="font-display text-3xl font-black text-ink-900 lg:text-4xl">
            Nova Proposta
          </h1>
        </div>

        {/* ── Form Card ────────────────────────────────────────────── */}
        <div className="rounded-2xl border border-secondary-200 bg-surface p-8 shadow-sm">
          <form onSubmit={handleSubmit} noValidate className="space-y-6">

            {/* Client Name */}
            <div className="space-y-1.5">
              <label
                htmlFor="clientName"
                className="block text-sm font-semibold text-ink-900"
              >
                Nome do Cliente <span className="text-red-500">*</span>
              </label>
              <input
                id="clientName"
                type="text"
                value={clientName}
                onChange={(e) => setClientName(e.target.value)}
                placeholder="ex: Acme Corp"
                className={`w-full rounded-xl border px-4 py-3 text-sm text-ink-900 bg-canvas placeholder:text-secondary-500 outline-none transition-colors focus:ring-2 focus:ring-primary-500 focus:border-primary-500 ${
                  errors.clientName
                    ? 'border-red-400'
                    : 'border-secondary-200 hover:border-secondary-400'
                }`}
              />
              {errors.clientName && (
                <p className="text-xs text-red-500">{errors.clientName}</p>
              )}
            </div>

            {/* Service Type */}
            <div className="space-y-1.5">
              <label
                htmlFor="serviceType"
                className="block text-sm font-semibold text-ink-900"
              >
                Tipo de Serviço <span className="text-red-500">*</span>
              </label>
              <select
                id="serviceType"
                value={serviceType}
                onChange={(e) => setServiceType(e.target.value)}
                className={`w-full rounded-xl border px-4 py-3 text-sm text-ink-900 bg-canvas outline-none transition-colors focus:ring-2 focus:ring-primary-500 focus:border-primary-500 ${
                  errors.serviceType
                    ? 'border-red-400'
                    : 'border-secondary-200 hover:border-secondary-400'
                } ${!serviceType ? 'text-secondary-500' : ''}`}
              >
                <option value="" disabled>
                  Selecione um serviço...
                </option>
                {SERVICE_TYPE_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
              {errors.serviceType && (
                <p className="text-xs text-red-500">{errors.serviceType}</p>
              )}
            </div>

            {/* Description (optional, UI only) */}
            <div className="space-y-1.5">
              <label
                htmlFor="description"
                className="block text-sm font-semibold text-ink-900"
              >
                Descrição{' '}
                <span className="text-secondary-500 font-normal">(opcional)</span>
              </label>
              <textarea
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={4}
                placeholder="Descreva brevemente o escopo inicial ou contexto do cliente..."
                className="w-full rounded-xl border border-secondary-200 px-4 py-3 text-sm text-ink-900 bg-canvas placeholder:text-secondary-500 outline-none resize-none transition-colors hover:border-secondary-400 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              />
            </div>

            {/* Actions */}
            <div className="flex flex-col-reverse gap-3 pt-2 sm:flex-row sm:justify-end">
              <Link
                href="/dashboard"
                className="inline-flex items-center justify-center rounded-xl border border-secondary-200 bg-surface px-5 py-3 text-sm font-bold text-ink-900 shadow-sm transition-all hover:bg-canvas hover:border-secondary-400"
              >
                Cancelar
              </Link>
              <button
                type="submit"
                className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary-500 px-5 py-3 text-sm font-bold text-ink-900 shadow-sm transition-all hover:bg-primary-400 hover:shadow"
              >
                <DocumentPlusIcon className="h-4 w-4" />
                Criar Proposta
              </button>
            </div>

          </form>
        </div>

      </div>
    </div>
  );
}
