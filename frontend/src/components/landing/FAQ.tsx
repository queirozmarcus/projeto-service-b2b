'use client';

import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { PlusIcon, MinusIcon } from '@heroicons/react/24/outline';

const FAQS = [
  {
    question: 'Como funciona o fluxo de briefing?',
    answer:
      'Você cria uma sessão de briefing no ScopeFlow e envia o link para o cliente. A IA conduz o cliente por perguntas estruturadas adaptadas ao tipo de serviço. Após as respostas, o sistema gera um escopo com entregáveis, premissas, exclusões e cronograma — pronto para sua revisão.',
  },
  {
    question: 'O cliente precisa criar uma conta?',
    answer:
      'Não. O cliente acessa o briefing e a proposta apenas pelo link que você envia — sem cadastro, sem fricção. Isso garante uma experiência limpa e profissional do lado do cliente.',
  },
  {
    question: 'Posso personalizar as perguntas do briefing?',
    answer:
      'Sim. Você pode editar o roteiro padrão de perguntas, criar perfis de contexto por tipo de serviço (ex: social media, landing page, branding) e definir entregáveis e exclusões padrão para cada nicho.',
  },
  {
    question: 'Como funciona a aprovação?',
    answer:
      'O cliente recebe um link com o escopo formatado. Ao aprovar, o sistema registra nome, e-mail, IP e timestamp. Qualquer edição posterior gera uma nova versão — mantendo um histórico imutável para referência futura.',
  },
  {
    question: 'A IA escreve o escopo inteiro?',
    answer:
      'A IA estrutura e consolida as respostas do briefing em um documento profissional. Você tem controle total para revisar, ajustar e aprovar antes de enviar para o cliente. O objetivo é acelerar, não substituir seu julgamento.',
  },
  {
    question: 'Posso usar minha identidade visual?',
    answer:
      'No plano Growth e Enterprise você pode adicionar logo, cores e nome do seu estúdio ou agência nas propostas. No Enterprise, é possível configurar ambiente white-label completo.',
  },
  {
    question: 'Preciso de cartão de crédito para testar?',
    answer:
      'Não. Você tem 14 dias grátis sem informar nenhum cartão. O plano de pagamento só é solicitado quando você decide continuar após o período de teste.',
  },
];

function FAQItem({ question, answer, isOpen, onToggle }: {
  question: string;
  answer: string;
  isOpen: boolean;
  onToggle: () => void;
}) {
  return (
    <div className="border-b border-dark-border last:border-0">
      <button
        onClick={onToggle}
        className="flex w-full items-center justify-between gap-6 py-5 text-left transition-colors hover:text-white/90"
      >
        <span className={`text-[15px] font-semibold transition-colors ${isOpen ? 'text-white' : 'text-white/65'}`}>
          {question}
        </span>
        <span className={`flex-shrink-0 flex h-7 w-7 items-center justify-center rounded-lg border transition-all duration-200 ${
          isOpen
            ? 'border-primary-500/40 bg-primary-500/10 text-primary-400'
            : 'border-dark-border bg-dark-raised text-white/30'
        }`}>
          {isOpen ? <MinusIcon className="h-3.5 w-3.5" /> : <PlusIcon className="h-3.5 w-3.5" />}
        </span>
      </button>

      <AnimatePresence initial={false}>
        {isOpen && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
            className="overflow-hidden"
          >
            <p className="pb-6 text-sm leading-relaxed text-white/40 font-medium max-w-2xl">
              {answer}
            </p>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

export function FAQ() {
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  return (
    <section id="faq" className="relative overflow-hidden bg-void px-6 py-28 border-t border-dark-border">
      <div className="pointer-events-none absolute inset-0">
        <div className="absolute right-0 top-1/2 h-[400px] w-[400px] -translate-y-1/2 translate-x-1/3 rounded-full bg-primary-500/4 blur-[120px]" />
      </div>

      <div className="relative mx-auto max-w-7xl">
        <div className="grid gap-16 lg:grid-cols-[0.6fr_1fr] lg:gap-24">
          {/* Left — header */}
          <div className="space-y-4 lg:sticky lg:top-24 lg:self-start">
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="flex items-center gap-3"
            >
              <span className="h-px w-8 bg-primary-500/40" />
              <span className="text-[11px] font-bold uppercase tracking-[0.22em] text-primary-500/70">
                FAQ
              </span>
            </motion.div>

            <motion.h2
              initial={{ opacity: 0, y: 16 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: 0.05 }}
              className="font-display text-3xl font-black leading-tight text-white sm:text-4xl"
            >
              Perguntas{' '}
              <span className="font-display italic text-primary-400">frequentes.</span>
            </motion.h2>

            <motion.p
              initial={{ opacity: 0, y: 12 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: 0.1 }}
              className="text-sm leading-relaxed text-white/35 font-medium"
            >
              Dúvida que não está aqui?{' '}
              <a href="mailto:oi@scopeflow.app" className="text-primary-400 hover:text-primary-300 transition-colors">
                Fale com a gente.
              </a>
            </motion.p>
          </div>

          {/* Right — accordion */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.1 }}
            className="rounded-2xl border border-dark-border bg-dark-surface px-7"
          >
            {FAQS.map((faq, i) => (
              <FAQItem
                key={i}
                question={faq.question}
                answer={faq.answer}
                isOpen={openIndex === i}
                onToggle={() => setOpenIndex(openIndex === i ? null : i)}
              />
            ))}
          </motion.div>
        </div>
      </div>
    </section>
  );
}
