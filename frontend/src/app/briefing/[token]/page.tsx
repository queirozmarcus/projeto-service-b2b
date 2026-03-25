import type { Metadata } from 'next';
import { BriefingFlow } from '@/components/briefing/BriefingFlow';
import type { BriefingSession } from '@/types/briefing';

// ---------------------------------------------------------------------------
// Metadata
// ---------------------------------------------------------------------------

export const metadata: Metadata = {
  title: 'Briefing — ScopeFlow',
  description: 'Answer a few questions so we can prepare an accurate proposal for you.',
  // Previne indexação da página de briefing (contém token único por cliente)
  robots: 'noindex, nofollow',
};

// ---------------------------------------------------------------------------
// Helpers de fetch server-side
// ---------------------------------------------------------------------------

/**
 * Recupera a BriefingSession pelo token público.
 *
 * Usa fetch nativo (Next.js cache-aware) em vez do axios client para
 * compatibilidade com o ambiente server-side (sem APIs de browser).
 *
 * GET /public/briefings/{token}
 *
 * @returns BriefingSession ou null se o token não existir / erro de rede
 */
async function fetchSessionByToken(token: string): Promise<BriefingSession | null> {
  // NEXT_PUBLIC_API_URL deve ser a base do servidor (ex: http://localhost:8080)
  // Endpoint público não usa /api/v1 prefix
  const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

  try {
    const res = await fetch(`${baseUrl}/public/briefings/${token}`, {
      // next: { revalidate: 0 } — sessão é dinâmica, nunca cacheada no server
      cache: 'no-store',
    });

    if (!res.ok) {
      // 404 → token inválido; outros códigos → tratar como inválido no MVP
      return null;
    }

    return (await res.json()) as BriefingSession;
  } catch {
    // Erro de rede — retorna null para exibir página de erro ao cliente
    return null;
  }
}

// ---------------------------------------------------------------------------
// Componente de erro (renderizado server-side)
// ---------------------------------------------------------------------------

function InvalidTokenPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-secondary-50 px-4">
      <div className="w-full max-w-md rounded-xl border border-secondary-200 bg-white p-8 text-center shadow-sm space-y-4">
        <div className="text-5xl" role="img" aria-label="Broken link">
          🔗
        </div>
        <h1 className="text-xl font-bold text-secondary-900">
          Invalid or Expired Link
        </h1>
        <p className="text-sm text-secondary-600 leading-relaxed">
          This briefing link is invalid or has already expired. Please contact
          the service provider to request a new link.
        </p>
      </div>
    </main>
  );
}

// ---------------------------------------------------------------------------
// Page — Server Component
// ---------------------------------------------------------------------------

interface BriefingPageProps {
  params: Promise<{ token: string }>;
}

/**
 * Página pública do briefing: /briefing/[token]
 *
 * Responsabilidades server-side:
 * 1. Validar que o token corresponde a uma sessão real
 * 2. Extrair o sessionId (UUID interno) necessário para o endpoint /complete
 * 3. Renderizar o client component BriefingFlow com os dados iniciais
 *
 * Sem autenticação: rota pública acessada pelo cliente do prestador de serviço.
 *
 * Nota sobre params: Next.js 15 tornou params uma Promise — await obrigatório.
 * Refs: https://nextjs.org/docs/app/api-reference/file-conventions/page
 */
export default async function BriefingPage({ params }: BriefingPageProps) {
  const { token } = await params;

  const session = await fetchSessionByToken(token);

  if (!session) {
    return <InvalidTokenPage />;
  }

  // Sessão já completada — não faz sentido exibir o fluxo novamente
  if (session.status === 'COMPLETED') {
    return (
      <main className="flex min-h-screen items-center justify-center bg-secondary-50 px-4">
        <div className="w-full max-w-md rounded-xl border border-secondary-200 bg-white p-8 text-center shadow-sm space-y-4">
          <div className="text-5xl" role="img" aria-label="Checkmark">
            ✅
          </div>
          <h1 className="text-xl font-bold text-secondary-900">
            Briefing Already Completed
          </h1>
          <p className="text-sm text-secondary-600">
            This briefing has already been submitted. Thank you!
          </p>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-gradient-to-br from-secondary-50 to-primary-50">
      <div className="container mx-auto max-w-2xl px-4 py-10 md:py-16">
        {/* Header */}
        <header className="mb-8 text-center">
          <p className="text-lg font-bold text-primary-600">ScopeFlow</p>
          <h1 className="mt-1 text-2xl font-bold text-secondary-900 md:text-3xl">
            Project Briefing
          </h1>
          <p className="mt-2 text-sm text-secondary-600">
            Answer the questions below so we can prepare a precise proposal for your project.
          </p>
        </header>

        {/* Card do fluxo */}
        <div className="rounded-xl border border-secondary-200 bg-white px-6 py-8 shadow-sm md:px-10">
          <BriefingFlow token={token} proposalId={session.proposalId || ''} />
        </div>
      </div>
    </main>
  );
}
