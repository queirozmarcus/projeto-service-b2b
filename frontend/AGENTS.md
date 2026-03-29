# Repository Guidelines

## Project Structure & Module Organization
This is a Next.js 15 frontend using the App Router. Main application code lives in `src/app`, shared UI in `src/components`, client state in `src/stores`, hooks in `src/hooks`, API helpers in `src/lib`, and shared types in `src/types`. Global styles are in `src/styles/globals.css`; static assets belong in `public/`. Unit tests are co-located under `src/**/__tests__`, and end-to-end coverage lives in `e2e/`.

## Build, Test, and Development Commands
Use Node 20+ and npm 10+.

- `npm run dev`: start the local Next.js dev server on `http://localhost:3000`.
- `npm run build`: create a production build.
- `npm run start`: serve the production build locally.
- `npm run lint`: run ESLint with zero warnings allowed.
- `npm run type-check`: run TypeScript without emitting files.
- `npm run test`: run Vitest unit/component tests.
- `npm run test:coverage`: run Vitest with coverage thresholds enforced.
- `npm run test:e2e`: run Playwright tests across configured browsers.
- `npm run format:check`: verify Prettier formatting.

## Coding Style & Naming Conventions
Prettier is the source of truth: 2 spaces, semicolons, single quotes, trailing commas (`es5`), 80-column width, and LF line endings. ESLint extends `next/core-web-vitals`; `any` is disallowed, `eqeqeq` is required, unused args must be prefixed with `_`, and `console` should be limited to `warn`/`error`. Use `PascalCase` for React components (`ProposalCard.tsx`), `camelCase` for hooks/utilities (`useBriefing.ts`), and keep test files as `*.test.tsx` or `*.spec.ts`.

## Testing Guidelines
Vitest runs in `jsdom` with Testing Library setup from `src/test/setup.ts`. Coverage targets are 80% for lines, functions, and statements, and 75% for branches over `src/components`, `src/hooks`, `src/stores`, and `src/lib`. Prefer co-located unit tests, for example `src/components/briefing/__tests__/QuestionCard.test.tsx`. Playwright tests run from `e2e/` and expect the app at `http://localhost:3000`; use `PLAYWRIGHT_TEST_BASE_URL` when pointing to another environment.

## Commit & Pull Request Guidelines
Recent history follows Conventional Commit style such as `feat(auth): ...`, `fix: ...`, and `docs(seo): ...`. Keep that format and scope changes when possible. PRs should include a short summary, linked issue/task, screenshots or short recordings for UI changes, and the commands you ran (`npm run lint`, `npm run test`, `npm run test:e2e` when relevant).

## Configuration Notes
Do not commit secrets. Keep environment-specific values outside the repo and review changes to `src/env.ts`, `src/lib/api.ts`, and `src/middleware.ts` carefully because they affect runtime access, routing, and API integration.
