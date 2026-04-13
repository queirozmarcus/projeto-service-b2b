/**
 * Tipos TypeScript para o domínio de Workspace Briefings (autenticados).
 *
 * Espelham exatamente os DTOs Java do backend:
 *   - BriefingResponse.java  (BriefingControllerV1)
 *   - ServiceType.java       (enum)
 *
 * IMPORTANTE: este arquivo cobre briefings gerenciados pelo service provider
 * via BriefingControllerV1 (POST /api/v1/briefings). É diferente de:
 *   - briefing.ts → BriefingSession usada no fluxo público do cliente
 *   - briefingSessionApi.ts → sessões vinculadas a proposals via BriefingSessionControllerV2
 */

// ---------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------

/** Espelha BriefingStatus.java — estados possíveis de um briefing autenticado */
export type WorkspaceBriefingStatus = 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED';

/**
 * Espelha ServiceType.java — tipos de serviço suportados pela plataforma.
 * Cada valor corresponde ao nome do enum no backend (serializado como string).
 */
export type ServiceType =
  | 'SOCIAL_MEDIA'
  | 'LANDING_PAGE'
  | 'WEB_DESIGN'
  | 'BRANDING'
  | 'VIDEO_PRODUCTION'
  | 'CONSULTING';

// ---------------------------------------------------------------------------
// Labels de exibição
// ---------------------------------------------------------------------------

/**
 * Labels legíveis para exibição no frontend — espelha os displayName de ServiceType.java.
 * Use sempre este mapa ao renderizar ServiceType em UI (selects, cards, badges).
 */
export const SERVICE_TYPE_LABELS: Record<ServiceType, string> = {
  SOCIAL_MEDIA: 'Social Media Management',
  LANDING_PAGE: 'Landing Page Design',
  WEB_DESIGN: 'Web Design',
  BRANDING: 'Branding',
  VIDEO_PRODUCTION: 'Video Production',
  CONSULTING: 'Consulting',
};

// ---------------------------------------------------------------------------
// Workspace Briefing aggregate
// ---------------------------------------------------------------------------

/**
 * Espelha BriefingResponse.java (record) retornado por BriefingControllerV1.
 *
 * Campos:
 *   - id           → UUID do briefing
 *   - workspaceId  → UUID do workspace dono
 *   - clientId     → UUID do cliente (gerado pelo frontend antes da chamada)
 *   - serviceType  → tipo do serviço solicitado
 *   - status       → estado atual do briefing
 *   - publicToken  → token para gerar o link que será enviado ao cliente
 *   - completionScore → score 0–100; null enquanto o briefing não for concluído
 *   - createdAt    → ISO 8601 (Instant serializado pelo backend)
 *   - updatedAt    → ISO 8601 (Instant serializado pelo backend)
 */
export interface WorkspaceBriefing {
  /** UUID */
  id: string;
  /** UUID do workspace dono do briefing */
  workspaceId: string;
  /** UUID do cliente associado (gerado pelo frontend) */
  clientId: string;
  serviceType: ServiceType;
  status: WorkspaceBriefingStatus;
  /** Token opaco para gerar o link público do cliente (ex: /briefing/{publicToken}) */
  publicToken: string;
  /** Score de 0 a 100; null até que o cliente conclua o briefing */
  completionScore: number | null;
  /** ISO 8601 — serializado a partir de Instant no backend */
  createdAt: string;
  /** ISO 8601 — serializado a partir de Instant no backend */
  updatedAt: string;
}

// ---------------------------------------------------------------------------
// Payloads de request
// ---------------------------------------------------------------------------

/** POST /api/v1/briefings */
export interface CreateWorkspaceBriefingPayload {
  /** UUID do cliente — gerado pelo frontend antes do POST */
  clientId: string;
  serviceType: ServiceType;
}

// ---------------------------------------------------------------------------
// Erros tipados
// ---------------------------------------------------------------------------

/**
 * Discriminated union de erros da Workspace Briefing API.
 * Trate com switch/case em `kind` — nunca faça type assertions sobre isso.
 *
 *   conflict     → 409: já existe briefing ativo para esse clientId + serviceType
 *   validation   → 400: payload inválido
 *   forbidden    → 403: workspace sem permissão
 *   server_error → 5xx: erro no servidor
 *   network      → sem resposta (timeout, offline)
 */
export type WorkspaceBriefingApiError =
  | { kind: 'conflict'; message: string }
  | { kind: 'validation'; message: string }
  | { kind: 'forbidden'; message: string }
  | { kind: 'server_error'; message: string }
  | { kind: 'network'; message: string };
