/**
 * Tipos TypeScript para o domínio de Proposals.
 *
 * Espelham exatamente os DTOs Java do backend:
 *   - ProposalResponse.java
 *   - ProposalPageResponse.java
 *   - ProposalScope.java (record)
 *
 * IMPORTANTE: o backend retorna `PUBLISHED` (não `SENT`).
 * Qualquer referência a `SENT` no frontend está incorreta e deve ser migrada.
 */

// ---------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------

/** Espelha ProposalStatus.java */
export type ProposalStatus = 'DRAFT' | 'PUBLISHED' | 'APPROVED' | 'REJECTED';

// ---------------------------------------------------------------------------
// Scope — value objects aninhados
// ---------------------------------------------------------------------------

/** Espelha o record Deliverable.java */
export interface Deliverable {
  name: string;
  description: string;
  acceptanceCriteria: string;
}

/** Espelha o record Price.java */
export interface Price {
  amount: number;
  currency: string;
  breakdown: string;
}

/** Espelha o record Milestone.java */
export interface Milestone {
  name: string;
  /** ISO 8601 date string (ex: "2026-06-30") */
  dueDate: string;
  description: string;
}

/** Espelha o record Timeline.java */
export interface Timeline {
  /** ISO 8601 date string */
  startDate: string;
  /** ISO 8601 date string */
  endDate: string;
  milestones: Milestone[];
}

/** Espelha o record ProposalScope.java */
export interface ProposalScope {
  deliverables: Deliverable[];
  exclusions: string[];
  assumptions: string[];
  price: Price | null;
  timeline: Timeline | null;
}

// ---------------------------------------------------------------------------
// Proposal aggregate
// ---------------------------------------------------------------------------

/** Espelha ProposalResponse.java */
export interface Proposal {
  /** UUID */
  id: string;
  /** UUID do workspace dono da proposta */
  workspaceId: string;
  /** UUID do cliente associado */
  clientId: string;
  /** UUID da sessão de briefing de origem */
  briefingId: string;
  proposalName: string;
  status: ProposalStatus;
  /** Scope gerado via AI; null até o primeiro updateScope */
  scope: ProposalScope | null;
  /** ISO 8601 — serializado a partir de Instant no backend */
  createdAt: string;
  /** ISO 8601 — serializado a partir de Instant no backend */
  updatedAt: string;
}

// ---------------------------------------------------------------------------
// Paginação
// ---------------------------------------------------------------------------

/** Espelha ProposalPageResponse.java */
export interface ProposalPage {
  content: Proposal[];
  totalElements: number;
  totalPages: number;
  size: number;
  /** Página atual — zero-based (Spring Page convention) */
  number: number;
  first: boolean;
  last: boolean;
}

// ---------------------------------------------------------------------------
// Payloads de request
// ---------------------------------------------------------------------------

/** POST /proposals */
export interface CreateProposalPayload {
  clientId: string;
  briefingId: string;
  proposalName: string;
}

/** PUT /proposals/{id} — só renomeia, disponível apenas em DRAFT */
export interface UpdateProposalPayload {
  proposalName: string;
}

/** POST /proposals/{id}/update-scope — disponível apenas em DRAFT */
export interface UpdateScopePayload {
  scope: ProposalScope;
}

/** POST /proposals/{id}/initiate-approval */
export interface InitiateApprovalPayload {
  approverEmails: string[];
}

// ---------------------------------------------------------------------------
// Query params
// ---------------------------------------------------------------------------

/** Parâmetros de query para GET /proposals */
export interface ListProposalsParams {
  /** Página zero-based */
  page?: number;
  size?: number;
  status?: ProposalStatus;
}

// ---------------------------------------------------------------------------
// Erros tipados
// ---------------------------------------------------------------------------

/**
 * Discriminated union de erros da Proposal API.
 * Trate com switch/case em `kind` — nunca faça type assertions sobre isso.
 */
export type ProposalApiError =
  | { kind: 'not_found'; message: string }
  | { kind: 'conflict'; message: string }
  | { kind: 'validation'; message: string }
  | { kind: 'forbidden'; message: string }
  | { kind: 'server_error'; message: string }
  | { kind: 'network'; message: string };
