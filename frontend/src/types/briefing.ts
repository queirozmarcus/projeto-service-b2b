/**
 * Briefing domain types — alinhados com os DTOs do backend.
 *
 * Fonte: BriefingQuestionResponse, BriefingCompletionResponse,
 *        BriefingSessionResponse, SubmitAnswersRequest.AnswerItem
 */

// ---------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------

/**
 * Tipo da questão conforme armazenado em ServiceContextQuestion.questionType.
 * O backend persiste como string livre; os valores conhecidos são listados aqui.
 * Valores inesperados devem ser tratados como 'TEXT' (fallback seguro).
 */
export type QuestionType = 'TEXT' | 'TEXTAREA' | 'SELECT' | 'CHECKBOX' | string;

/**
 * Status de uma BriefingSession.
 * Mapeado diretamente do campo `status` retornado pelo backend.
 */
export type BriefingStatus = 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED';

// ---------------------------------------------------------------------------
// API response shapes (espelho dos Java records)
// ---------------------------------------------------------------------------

/**
 * Espelho de BriefingQuestionResponse.java
 *
 * GET /public/briefings/{token}/questions → List<BriefingQuestionResponse>
 *
 * Campos: questionId (UUID), questionText, type (string), orderIndex, required
 */
export interface Question {
  /** UUID da pergunta no ServiceContextProfile */
  questionId: string;
  /** Texto exibido ao cliente */
  questionText: string;
  /**
   * Tipo de input. Use QuestionType para os valores conhecidos.
   * Trate valores desconhecidos como 'TEXT' para manter compatibilidade futura.
   */
  type: QuestionType;
  /** Posição no fluxo (base 0, ordenado pelo backend) */
  orderIndex: number;
  /** Se verdadeiro, é obrigatório para calcular completeness score */
  required: boolean;
}

/**
 * Par questionId → answerText enviado ao backend.
 * Espelho de SubmitAnswersRequest.AnswerItem.java
 *
 * POST /public/briefings/{token}/batch-answers → 204 No Content
 */
export interface Answer {
  /** UUID da pergunta (deve coincidir com Question.questionId) */
  questionId: string;
  /** Resposta digitada pelo cliente (não pode ser vazio) */
  answerText: string;
}

/**
 * Body enviado em POST /public/briefings/{token}/batch-answers
 */
export interface SubmitAnswersPayload {
  answers: Answer[];
}

/**
 * Espelho de BriefingCompletionResponse.java
 *
 * POST /api/v1/briefing-sessions/{id}/complete → 200 OK
 *
 * Nota: o campo se chama `completenessScore` (não `completionScore`).
 */
export interface CompletionResult {
  /** Percentual de completude calculado pelo backend (0-100) */
  completenessScore: number;
  /** Sempre "COMPLETED" neste response */
  status: 'COMPLETED';
  /**
   * Mensagem gerada pelo backend:
   * - score >= 80 → "Briefing completed successfully..."
   * - score < 80  → "Briefing completed with low score (X%)..."
   */
  message: string;
}

/**
 * Espelho de BriefingSessionResponse.java
 *
 * Retornado por:
 * - POST /api/v1/proposals/{proposalId}/briefing-sessions → 201 Created
 * - GET  /api/v1/briefing-sessions/{id}                  → 200 OK
 * - GET  /api/v1/briefing-sessions/token/{token}         → 200 OK (autenticado)
 */
export interface BriefingSession {
  id: string;
  proposalId: string | null;
  status: BriefingStatus;
  publicToken: string;
  /** null enquanto a sessão não foi completada */
  completenessScore: number | null;
  createdAt: string;
  updatedAt: string;
}

// ---------------------------------------------------------------------------
// UI state shapes (usados pelo store e pelo hook)
// ---------------------------------------------------------------------------

/**
 * Map de respostas coletadas localmente durante o fluxo.
 * Chave: Question.questionId | Valor: texto digitado pelo cliente
 */
export type AnswersMap = Map<string, string>;

/**
 * Erros normalizados da camada de API.
 * Permite que o hook e o store diferenciem o tipo de falha para exibir
 * mensagens específicas sem acoplar o componente ao código HTTP.
 */
export type BriefingApiError =
  | { kind: 'token_invalid'; message: string }       // 404
  | { kind: 'session_completed'; message: string }   // 409
  | { kind: 'validation'; message: string }          // 400
  | { kind: 'server_error'; message: string }        // 5xx
  | { kind: 'network'; message: string };            // sem resposta HTTP
