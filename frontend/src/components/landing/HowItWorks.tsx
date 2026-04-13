'use client';

import { motion } from 'framer-motion';
import { CheckCircleIcon, ArrowRightIcon } from '@heroicons/react/24/outline';

const STEPS = [
  {
    number: '01',
    label: 'Diagnóstico',
    title: 'Perguntas que revelam o real objetivo',
    description:
      'A IA conduz um roteiro de perguntas adaptadas ao tipo de serviço. Nada genérico — cada pergunta tem propósito. O cliente responde no próprio ritmo, pelo link que você envia.',
    highlights: ['Fluxo adaptável por nicho', 'Sem formulário genérico', 'Link de briefing compartilhável'],
    icon: '🔍',
  },
  {
    number: '02',
    label: 'Consolidação',
    title: 'Respostas viram documento executivo',
    description:
      'A IA transforma as respostas em um escopo estruturado com entregáveis, premissas, exclusões, cronograma e critérios claros. Você revisa e ajusta antes de enviar.',
    highlights: ['Escopo com linguagem profissional', 'Entregáveis e exclusões claros', 'Revisão antes de enviar'],
    icon: '📋',
  },
  {
    number: '03',
    label: 'Aprovação',
    title: 'Aceite rastreável, sem ruído',
    description:
      'O cliente aprova com nome, e-mail, IP e timestamp registrados. Qualquer alteração gera nova versão. Você tem um histórico imutável para qualquer discussão futura.',
    highlights: ['Aprovação com rastreabilidade', 'Histórico versionado', 'PDF gerado automaticamente'],
    icon: '✓',
  },
];

export function HowItWorks() {
  const containerVariants = {
    hidden: {},
    visible: { transition: { staggerChildren: 0.15, delayChildren: 0.1 } },
  };

  return (
    <section
      id="how-it-works"
      className="relative overflow-hidden bg-void px-6 py-32 border-t border-dark-border"
    >
      {/* Background elements */}
      <div className="pointer-events-none absolute inset-0">
        {/* Top accent line */}
        <div className="absolute left-0 right-0 top-0 h-px bg-gradient-to-r from-transparent via-primary-500/20 to-transparent" />
        {/* Subtle glow */}
        <div className="absolute left-1/3 top-1/4 h-96 w-96 rounded-full bg-secondary-500/4 blur-[120px]" />
      </div>

      <div className="relative mx-auto max-w-7xl">
        {/* Header */}
        <div className="mb-20 space-y-6">
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="flex items-center gap-3"
          >
            <span className="h-px w-8 bg-primary-500/40" />
            <span className="text-[11px] font-bold uppercase tracking-[0.22em] text-primary-500/70">
              Como funciona
            </span>
          </motion.div>

          <motion.h2
            initial={{ opacity: 0, y: 16 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.05 }}
            className="max-w-3xl font-display text-5xl font-black leading-tight text-white sm:text-6xl"
          >
            Três etapas. Da conversa ao{' '}
            <span className="font-display italic text-primary-400">aceite formal.</span>
          </motion.h2>

          <motion.p
            initial={{ opacity: 0, y: 12 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.1 }}
            className="max-w-2xl text-base text-white/40 font-medium leading-relaxed"
          >
            Cada etapa é projetada para transmitir confiança, reduzir ambiguidade e
            acelerar a decisão do cliente. Sem burocracia, sem idas e vindas.
          </motion.p>
        </div>

        {/* Steps Timeline */}
        <motion.div
          variants={containerVariants}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true, margin: '-80px' }}
          className="relative"
        >
          {/* Desktop connecting line */}
          <div className="hidden lg:block absolute top-[60px] left-0 right-0 h-px bg-gradient-to-r from-transparent via-primary-500/30 to-transparent" />

          <div className="grid gap-8 lg:grid-cols-3">
            {STEPS.map((step, i) => (
              <motion.div
                key={i}
                variants={{
                  hidden: { opacity: 0, y: 20 },
                  visible: { opacity: 1, y: 0, transition: { duration: 0.6 } },
                }}
                className="group relative"
              >
                {/* Step card */}
                <div className="rounded-xl border border-dark-border bg-dark-surface/40 backdrop-blur-sm p-7 transition-all duration-300 hover:border-primary-500/40 hover:bg-dark-surface/80">
                  {/* Header with number */}
                  <div className="mb-6 flex items-start justify-between">
                    {/* Iconic number indicator */}
                    <motion.div
                      whileHover={{ scale: 1.1, rotate: -5 }}
                      className="relative"
                    >
                      <div className="flex h-16 w-16 items-center justify-center rounded-lg border border-primary-500/25 bg-gradient-to-br from-primary-500/15 to-primary-500/5 font-display text-2xl font-black text-primary-400 transition-all group-hover:border-primary-500/40 group-hover:from-primary-500/25">
                        {step.icon}
                      </div>
                      {/* Step number badge */}
                      <div className="absolute -top-2 -right-2 flex h-7 w-7 items-center justify-center rounded-full border border-primary-500/40 bg-primary-500/10 text-[10px] font-bold text-primary-300">
                        {step.number}
                      </div>
                    </motion.div>

                    {/* Stage label */}
                    <span className="text-[10px] font-bold uppercase tracking-widest text-white/30 group-hover:text-white/50 transition-colors">
                      {step.label}
                    </span>
                  </div>

                  {/* Content */}
                  <div className="space-y-4">
                    <h3 className="font-display text-lg font-bold text-white leading-snug group-hover:text-white transition-colors">
                      {step.title}
                    </h3>

                    <p className="text-sm leading-relaxed text-white/50 font-medium">
                      {step.description}
                    </p>

                    {/* Highlights with checkmarks */}
                    <ul className="space-y-2 border-t border-dark-border pt-4">
                      {step.highlights.map((h) => (
                        <li key={h} className="flex items-start gap-2.5 text-[12px] font-medium text-white/60">
                          <CheckCircleIcon className="h-3.5 w-3.5 flex-shrink-0 text-accent-500/70 mt-0.5" />
                          <span>{h}</span>
                        </li>
                      ))}
                    </ul>
                  </div>

                  {/* Forward indicator */}
                  {i < STEPS.length - 1 && (
                    <div className="hidden lg:flex absolute -right-4 top-1/2 -translate-y-1/2 items-center justify-center">
                      <motion.div
                        animate={{ x: [0, 4, 0] }}
                        transition={{ duration: 2, repeat: Infinity }}
                        className="flex h-8 w-8 items-center justify-center rounded-full border border-primary-500/20 bg-void text-primary-500/40"
                      >
                        <ArrowRightIcon className="h-4 w-4" />
                      </motion.div>
                    </div>
                  )}
                </div>
              </motion.div>
            ))}
          </div>
        </motion.div>

        {/* Bottom CTA hint */}
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ delay: 0.5 }}
          className="mt-16 rounded-xl border border-primary-500/20 bg-primary-500/5 p-6 text-center backdrop-blur-sm"
        >
          <p className="text-sm font-medium text-white/70">
            Pronto para começar? <span className="text-primary-400 font-semibold">Teste gratuitamente por 14 dias</span> — sem cartão de crédito.
          </p>
        </motion.div>
      </div>
    </section>
  );
}
