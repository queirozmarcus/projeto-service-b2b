'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  ChevronLeftIcon,
  DocumentPlusIcon,
  CheckCircleIcon,
  ClipboardDocumentIcon,
} from '@heroicons/react/24/outline';
import { proposalApi } from '@/lib/proposalApi';
import { workspaceBriefingApi } from '@/lib/workspaceBriefingApi';
import {
  SERVICE_TYPE_LABELS,
  type ServiceType,
  type WorkspaceBriefing,
} from '@/types/workspace-briefing';

// ---------------------------------------------------------------------------
// Constantes
// ---------------------------------------------------------------------------

const SERVICE_TYPE_OPTIONS = Object.entries(SERVICE_TYPE_LABELS) as [ServiceType, string][];

// ---------------------------------------------------------------------------
// Tipos internos de estado
// ---------------------------------------------------------------------------

type WizardStep = 'create_briefing' | 'create_proposal';

interface Step1Fields {
  clientName: string;
  serviceType: ServiceType | '';
}

interface Step1Errors {
  clientName?: string;
  serviceType?: string;
  api?: string;
}

interface Step2Fields {
  proposalName: string;
}

interface Step2Errors {
  api?: string;
}

// ---------------------------------------------------------------------------
// Componente principal
// ---------------------------------------------------------------------------

export default function NewProposalPage() {
  const router = useRouter();

  // Wizard state
  const [step, setStep] = useState<WizardStep>('create_briefing');

  // Step 1 — criar briefing
  const [step1, setStep1] = useState<Step1Fields>({ clientName: '', serviceType: '' });
  const [step1Errors, setStep1Errors] = useState<Step1Errors>({});
  const [isCreatingBriefing, setIsCreatingBriefing] = useState(false);

  // clientId gerado uma vez no início e reutilizado na Etapa 2
  const [clientId, setClientId] = useState<string>('');

  // Briefing criado na Etapa 1 — usado na Etapa 2
  const [createdBriefing, setCreatedBriefing] = useState<WorkspaceBriefing | null>(null);

  // Step 2 — criar proposta
  const [step2, setStep2] = useState<Step2Fields>({ proposalName: '' });
  const [step2Errors, setStep2Errors] = useState<Step2Errors>({});
  const [isCreatingProposal, setIsCreatingProposal] = useState(false);

  // Controle de cópia do link público
  const [linkCopied, setLinkCopied] = useState(false);

  // ---------------------------------------------------------------------------
  // Step 1 — handlers
  // ---------------------------------------------------------------------------

  function validateStep1(): boolean {
    const next: Step1Errors = {};
    if (!step1.clientName.trim()) next.clientName = 'Nome do cliente é obrigatório.';
    if (!step1.serviceType) next.serviceType = 'Selecione um tipo de serviço.';
    setStep1Errors(next);
    return Object.keys(next).length === 0;
  }

  async function handleCreateBriefing(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!validateStep1()) return;

    // Gera clientId uma única vez por fluxo
    const newClientId = crypto.randomUUID();
    setClientId(newClientId);

    setIsCreatingBriefing(true);
    setStep1Errors({});

    try {
      const briefing = await workspaceBriefingApi.create({
        clientId: newClientId,
        serviceType: step1.serviceType as ServiceType,
      });

      setCreatedBriefing(briefing);

      // Pré-preenche o nome da proposta com clientName + serviceType label
      const defaultProposalName =
        `${step1.clientName.trim()} — ${SERVICE_TYPE_LABELS[briefing.serviceType]}`;
      setStep2({ proposalName: defaultProposalName });

      setStep('create_proposal');
    } catch (err) {
      // err é WorkspaceBriefingApiError (discriminated union)
      const apiError = err as { kind: string; message: string };
      setStep1Errors({ api: apiError.message });
    } finally {
      setIsCreatingBriefing(false);
    }
  }

  // ---------------------------------------------------------------------------
  // Step 2 — handlers
  // ---------------------------------------------------------------------------

  async function handleCreateProposal(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!createdBriefing) return;

    setIsCreatingProposal(true);
    setStep2Errors({});

    try {
      const proposal = await proposalApi.create({
        clientId,
        briefingId: createdBriefing.id,
        proposalName: step2.proposalName.trim(),
      });

      router.push(`/dashboard/proposals/${proposal.id}`);
    } catch (err) {
      // err é ProposalApiError (discriminated union)
      const apiError = err as { kind: string; message: string };
      setStep2Errors({ api: apiError.message });
    } finally {
      setIsCreatingProposal(false);
    }
  }

  async function handleCopyLink() {
    if (!createdBriefing) return;
    const url = `${window.location.origin}/briefing/${createdBriefing.publicToken}`;
    await navigator.clipboard.writeText(url);
    setLinkCopied(true);
    setTimeout(() => setLinkCopied(false), 2000);
  }

  // ---------------------------------------------------------------------------
  // Render — estrutura compartilhada (breadcrumb + header)
  // ---------------------------------------------------------------------------

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

        {/* ── Etapa 1: Criar Briefing ───────────────────────────────── */}
        {step === 'create_briefing' && (
          <div className="rounded-2xl border border-secondary-200 bg-surface p-8 shadow-sm">
            <div className="mb-6 space-y-1">
              <h2 className="text-lg font-bold text-ink-900">Etapa 1 — Iniciar Briefing</h2>
              <p className="text-sm text-secondary-500">
                Informe o cliente e o tipo de serviço. Um link de briefing será gerado para você
                compartilhar com o cliente.
              </p>
            </div>

            <form onSubmit={handleCreateBriefing} noValidate className="space-y-6">

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
                  value={step1.clientName}
                  onChange={(e) => setStep1({ ...step1, clientName: e.target.value })}
                  placeholder="ex: Acme Corp"
                  disabled={isCreatingBriefing}
                  className={`w-full rounded-xl border px-4 py-3 text-sm text-ink-900 bg-canvas placeholder:text-secondary-500 outline-none transition-colors focus:ring-2 focus:ring-primary-500 focus:border-primary-500 disabled:opacity-60 disabled:cursor-not-allowed ${
                    step1Errors.clientName
                      ? 'border-red-400'
                      : 'border-secondary-200 hover:border-secondary-400'
                  }`}
                />
                {step1Errors.clientName && (
                  <p className="text-xs text-red-500">{step1Errors.clientName}</p>
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
                  value={step1.serviceType}
                  onChange={(e) =>
                    setStep1({ ...step1, serviceType: e.target.value as ServiceType })
                  }
                  disabled={isCreatingBriefing}
                  className={`w-full rounded-xl border px-4 py-3 text-sm text-ink-900 bg-canvas outline-none transition-colors focus:ring-2 focus:ring-primary-500 focus:border-primary-500 disabled:opacity-60 disabled:cursor-not-allowed ${
                    step1Errors.serviceType
                      ? 'border-red-400'
                      : 'border-secondary-200 hover:border-secondary-400'
                  } ${!step1.serviceType ? 'text-secondary-500' : ''}`}
                >
                  <option value="" disabled>
                    Selecione um serviço...
                  </option>
                  {SERVICE_TYPE_OPTIONS.map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
                {step1Errors.serviceType && (
                  <p className="text-xs text-red-500">{step1Errors.serviceType}</p>
                )}
              </div>

              {/* Erro de API inline */}
              {step1Errors.api && (
                <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3">
                  <p className="text-sm text-red-600">{step1Errors.api}</p>
                </div>
              )}

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
                  disabled={isCreatingBriefing}
                  className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary-500 px-5 py-3 text-sm font-bold text-ink-900 shadow-sm transition-all hover:bg-primary-400 hover:shadow disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  <DocumentPlusIcon className="h-4 w-4" />
                  {isCreatingBriefing ? 'Criando...' : 'Criar Briefing'}
                </button>
              </div>

            </form>
          </div>
        )}

        {/* ── Etapa 2: Briefing criado + Criar Proposta ────────────── */}
        {step === 'create_proposal' && createdBriefing && (
          <div className="space-y-6">

            {/* Banner de sucesso */}
            <div className="flex items-center gap-3 rounded-2xl border border-green-200 bg-green-50 px-6 py-4">
              <CheckCircleIcon className="h-6 w-6 flex-shrink-0 text-green-600" />
              <p className="text-sm font-semibold text-green-700">
                Briefing criado com sucesso!
              </p>
            </div>

            {/* Card — Link para o cliente */}
            <div className="rounded-2xl border border-secondary-200 bg-surface p-8 shadow-sm space-y-4">
              <div className="space-y-1">
                <h2 className="text-base font-bold text-ink-900">Link para o cliente</h2>
                <p className="text-sm text-secondary-500">
                  Compartilhe o link abaixo com o cliente. Quando ele concluir o briefing, crie
                  a proposta abaixo.
                </p>
              </div>

              {/* URL do link público */}
              <div className="flex items-center gap-3 rounded-xl border border-secondary-200 bg-canvas px-4 py-3">
                <span className="flex-1 truncate text-sm text-ink-900 font-mono">
                  {typeof window !== 'undefined'
                    ? `${window.location.origin}/briefing/${createdBriefing.publicToken}`
                    : `/briefing/${createdBriefing.publicToken}`}
                </span>
                <button
                  type="button"
                  onClick={handleCopyLink}
                  className="inline-flex flex-shrink-0 items-center gap-1.5 rounded-lg border border-secondary-200 bg-surface px-3 py-1.5 text-xs font-semibold text-ink-900 transition-all hover:bg-canvas hover:border-secondary-400"
                >
                  <ClipboardDocumentIcon className="h-4 w-4" />
                  {linkCopied ? 'Copiado!' : 'Copiar'}
                </button>
              </div>
            </div>

            {/* Separador */}
            <div className="flex items-center gap-4">
              <div className="flex-1 border-t border-secondary-200" />
              <span className="text-xs font-semibold uppercase tracking-wider text-secondary-400">
                Criar Proposta
              </span>
              <div className="flex-1 border-t border-secondary-200" />
            </div>

            {/* Card — Criar Proposta */}
            <div className="rounded-2xl border border-secondary-200 bg-surface p-8 shadow-sm">
              <div className="mb-6 space-y-1">
                <h2 className="text-lg font-bold text-ink-900">Etapa 2 — Criar Proposta</h2>
                <p className="text-sm text-secondary-500">
                  Confirme o nome da proposta e clique em criar. Você poderá editar os detalhes
                  depois.
                </p>
              </div>

              <form onSubmit={handleCreateProposal} noValidate className="space-y-6">

                {/* Proposal Name */}
                <div className="space-y-1.5">
                  <label
                    htmlFor="proposalName"
                    className="block text-sm font-semibold text-ink-900"
                  >
                    Nome da Proposta <span className="text-red-500">*</span>
                  </label>
                  <input
                    id="proposalName"
                    type="text"
                    value={step2.proposalName}
                    onChange={(e) => setStep2({ proposalName: e.target.value })}
                    disabled={isCreatingProposal}
                    className="w-full rounded-xl border border-secondary-200 px-4 py-3 text-sm text-ink-900 bg-canvas placeholder:text-secondary-500 outline-none transition-colors hover:border-secondary-400 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 disabled:opacity-60 disabled:cursor-not-allowed"
                  />
                </div>

                {/* Erro de API inline */}
                {step2Errors.api && (
                  <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3">
                    <p className="text-sm text-red-600">{step2Errors.api}</p>
                  </div>
                )}

                {/* Actions */}
                <div className="flex flex-col-reverse gap-3 pt-2 sm:flex-row sm:justify-end">
                  <Link
                    href="/dashboard"
                    className="inline-flex items-center justify-center rounded-xl border border-secondary-200 bg-surface px-5 py-3 text-sm font-bold text-ink-900 shadow-sm transition-all hover:bg-canvas hover:border-secondary-400"
                  >
                    Ir para o Dashboard
                  </Link>
                  <button
                    type="submit"
                    disabled={isCreatingProposal || !step2.proposalName.trim()}
                    className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary-500 px-5 py-3 text-sm font-bold text-ink-900 shadow-sm transition-all hover:bg-primary-400 hover:shadow disabled:opacity-60 disabled:cursor-not-allowed"
                  >
                    <DocumentPlusIcon className="h-4 w-4" />
                    {isCreatingProposal ? 'Criando...' : 'Criar Proposta'}
                  </button>
                </div>

              </form>
            </div>

          </div>
        )}

      </div>
    </div>
  );
}
