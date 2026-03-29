# Layout Feedback — ScopeFlow Landing Page
**Data:** 2026-03-29
**URL:** http://localhost:3000
**Analisado por:** Marcus (via Playwright headless screenshots)

---

## 🔴 Problema Crítico: CSS Zero — Tailwind v4 Não Está Sendo Processado

### Sintoma
A página renderiza completamente sem estilo: fundo branco, texto preto, ícones HeroIcons
em tamanho original (sem `h-*`/`w-*` aplicados), nenhuma cor ou layout aplicado.

### Causa Raiz
O projeto usa **Tailwind v4** (`tailwindcss: ^4.0.0`) com a nova sintaxe CSS-first:

```css
/* globals.css */
@import "tailwindcss";
@theme { ... }
```

Mas faltam dois itens **obrigatórios** para o v4 funcionar com Next.js:

| Item Faltando | Por Quê É Necessário |
|---------------|---------------------|
| `@tailwindcss/postcss` (npm package) | Tailwind v4 usa um plugin PostCSS separado; o `tailwindcss` antigo não processa `@import "tailwindcss"` |
| `postcss.config.mjs` | Sem ele, o Next.js não registra o plugin e ignora o CSS inteiro |

### Problema Secundário — `tailwind.config.js` Conflitante (v3 Legacy)
O arquivo `tailwind.config.js` tem config v3 **incompatível** com o que o código espera:
- **No config:** `primary = azul/sky (#0ea5e9)`
- **No código:** `primary = âmbar (#F5A623)` → definido no `@theme {}` do CSS

Em v4, o `tailwind.config.js` é **ignorado** — o `@theme {}` no CSS é a fonte de verdade.
O arquivo deve ser removido para evitar confusão.

### Bug Adicional — Animação Indefinida
`animate-ambient-pulse` é usada no `Hero.tsx` mas não está definida em nenhum lugar
(nem no `tailwind.config.js`, nem no `globals.css`). Quebraria mesmo com CSS funcionando.

---

## ✅ Pontos Positivos (análise de código — design intencional)

### Navbar
- Glassmorphism ao scroll: `backdrop-blur-xl` + `bg-void/80` — elegante e moderno
- Logo com ambient glow animado — boa identidade visual
- Animação de entrada com Framer Motion

### Hero Section
- Grid 2 colunas: copy persuasivo + mockup de produto — estrutura ideal para conversão
- Mockup da proposta com detalhes realistas (status badge pulsante, entregáveis, stats)
- Waitlist form integrado diretamente no Hero — bom para captura de lead imediata
- Social proof (avatares + contagem) abaixo do form — reduz fricção
- Paleta dark coerente: base `void` (#09090E), amber como primary, emerald como accent

### ProblemSolution
- Antes/Depois com contraste vermelho vs verde — muito persuasivo
- Stats de impacto destacados: "40% retrabalho" vs "2.1x conversão"

### HowItWorks
- Timeline horizontal com 3 etapas numeradas
- Connecting line animada no desktop
- Highlights com checkmarks por etapa

### Geral
- Framer Motion com `whileInView` para scroll animations — bem executado
- Design tokens coerentes no `@theme {}` do globals.css
- Tipografia display (Fraunces) + sans (DM Sans) — escolha sofisticada

---

## 🛠️ Correções Necessárias

### P0 — CSS Quebrado (bloqueante)
1. `npm install @tailwindcss/postcss`
2. Criar `postcss.config.mjs`:
   ```js
   export default { plugins: { "@tailwindcss/postcss": {} } };
   ```
3. Remover `tailwind.config.js` (v3 legacy)

### P1 — Bug de Animação
4. Definir `animate-ambient-pulse` no `globals.css`:
   ```css
   @keyframes ambient-pulse {
     0%, 100% { opacity: 0.6; transform: scale(1); }
     50% { opacity: 1; transform: scale(1.05); }
   }
   ```

### P2 — Dashboard e demais páginas
5. Verificar se `dashboard/page.tsx` e outros layouts também usam classes v4-only
6. Confirmar que componentes de UI Radix recebem classes corretamente

---

## Resultado Esperado Após Fix
- Dark landing com fundo `#09090E`
- Amber (#F5A623) como cor primária
- Animações Framer Motion visíveis
- Layout responsivo (grid 2-col no desktop, 1-col no mobile)
- Mockup de proposta visível somente em `lg:` breakpoint
