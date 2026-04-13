'use client';

import { motion } from 'framer-motion';
import {
  XMarkIcon,
  CheckIcon,
  ChatBubbleBottomCenterIcon,
  DocumentIcon,
  SparklesIcon,
} from '@heroicons/react/24/outline';

export function ProblemSolution() {
  const containerVariants = {
    hidden: {},
    visible: { transition: { staggerChildren: 0.15, delayChildren: 0.1 } },
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.6 } },
  };

  return (
    <section className="relative overflow-hidden bg-void px-6 py-32 border-t border-dark-border">
      {/* Background structure */}
      <div className="pointer-events-none absolute inset-0">
        {/* Accent glow */}
        <div className="absolute -left-32 top-1/3 h-96 w-96 rounded-full bg-primary-500/5 blur-[120px]" />
        <div className="absolute -right-32 bottom-1/3 h-96 w-96 rounded-full bg-secondary-500/5 blur-[120px]" />
        {/* Subtle grid */}
        <div className="absolute inset-0 bg-[linear-gradient(90deg,#FFFFFF04_0.5px,transparent_0.5px)] bg-[size:64px_auto]" />
      </div>

      <div className="relative mx-auto max-w-7xl">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="mb-20 space-y-6 text-center"
        >
          <div className="inline-flex items-center gap-2 rounded-full border border-danger-500/20 bg-danger-500/8 px-4 py-2">
            <span className="text-[11px] font-bold uppercase tracking-widest text-danger-500/80">
              O Problema Real
            </span>
          </div>

          <h2 className="mx-auto max-w-3xl font-display text-5xl font-black leading-tight text-white sm:text-6xl lg:text-7xl">
            Conversas vagas viram{' '}
            <span className="font-display italic text-danger-500">escopos ruins.</span>
          </h2>

          <p className="mx-auto max-w-2xl text-lg text-white/40 font-medium leading-relaxed">
            Freelancers e agências enfrentam o mesmo ciclo: descoberta confusa, escopo
            ambíguo, aprovação lenta, retrabalho garantido.
          </p>
        </motion.div>

        {/* Before / After Grid */}
        <motion.div
          variants={containerVariants}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true, margin: '-80px' }}
          className="grid gap-12 lg:grid-cols-2 lg:gap-16"
        >
          {/* BEFORE */}
          <motion.div variants={itemVariants} className="space-y-6">
            <div className="flex items-center gap-3">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg border border-danger-500/30 bg-danger-500/10">
                <XMarkIcon className="h-4 w-4 text-danger-500" />
              </div>
              <h3 className="font-display text-2xl font-bold text-white">Antes</h3>
            </div>

            <div className="space-y-4">
              {[
                {
                  icon: ChatBubbleBottomCenterIcon,
                  title: 'Descoberta no WhatsApp',
                  desc: 'Mensagens desorganizadas, notas de áudio perdidas',
                },
                {
                  icon: DocumentIcon,
                  title: 'Escopo genérico',
                  desc: 'PDF template sem contexto, entregáveis vagos',
                },
                {
                  icon: XMarkIcon,
                  title: 'Aprovação lenta',
                  desc: 'Email, reunião, ajustes, mais email, mais reunião',
                },
              ].map((item, i) => {
                const Icon = item.icon;
                return (
                  <motion.div
                    key={i}
                    variants={itemVariants}
                    className="rounded-xl border border-dark-border bg-dark-raised/50 p-4 backdrop-blur-sm"
                  >
                    <div className="flex gap-3">
                      <Icon className="h-5 w-5 flex-shrink-0 text-danger-500/60 mt-0.5" />
                      <div>
                        <p className="font-semibold text-white/80">{item.title}</p>
                        <p className="text-sm text-white/40 mt-1">{item.desc}</p>
                      </div>
                    </div>
                  </motion.div>
                );
              })}
            </div>

            {/* Impact stat */}
            <div className="rounded-xl border border-danger-500/20 bg-danger-500/8 p-5 backdrop-blur-sm">
              <p className="text-[11px] font-semibold uppercase tracking-widest text-danger-500/70 mb-2">
                Impacto
              </p>
              <p className="text-3xl font-black text-danger-400">
                40% de retrabalho em média
              </p>
              <p className="text-sm text-danger-400/60 mt-2">
                Tempo dobrado, cliente insatisfeito, margem erodida
              </p>
            </div>
          </motion.div>

          {/* AFTER */}
          <motion.div variants={itemVariants} className="space-y-6">
            <div className="flex items-center gap-3">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg border border-accent-500/30 bg-accent-500/10">
                <CheckIcon className="h-4 w-4 text-accent-500" />
              </div>
              <h3 className="font-display text-2xl font-bold text-white">Com ScopeFlow</h3>
            </div>

            <div className="space-y-4">
              {[
                {
                  icon: SparklesIcon,
                  title: 'Descoberta guiada por IA',
                  desc: 'Sistema faz perguntas inteligentes, consolida automaticamente',
                },
                {
                  icon: DocumentIcon,
                  title: 'Escopo profissional',
                  desc: 'Documento estruturado com entregáveis, exclusões e timeline',
                },
                {
                  icon: CheckIcon,
                  title: 'Aprovação em minutos',
                  desc: 'Link compartilhável, contexto claro, cliente aprova direto',
                },
              ].map((item, i) => {
                const Icon = item.icon;
                return (
                  <motion.div
                    key={i}
                    variants={itemVariants}
                    className="rounded-xl border border-accent-500/20 bg-accent-500/8 p-4 backdrop-blur-sm"
                  >
                    <div className="flex gap-3">
                      <Icon className="h-5 w-5 flex-shrink-0 text-accent-500 mt-0.5" />
                      <div>
                        <p className="font-semibold text-white/80">{item.title}</p>
                        <p className="text-sm text-white/40 mt-1">{item.desc}</p>
                      </div>
                    </div>
                  </motion.div>
                );
              })}
            </div>

            {/* Success stat */}
            <div className="rounded-xl border border-accent-500/20 bg-accent-500/8 p-5 backdrop-blur-sm">
              <p className="text-[11px] font-semibold uppercase tracking-widest text-accent-500/70 mb-2">
                Resultado
              </p>
              <p className="text-3xl font-black text-accent-300">
                2.1x mais conversão
              </p>
              <p className="text-sm text-accent-300/60 mt-2">
                Aprovações mais rápidas, menos retrabalho, margens maiores
              </p>
            </div>
          </motion.div>
        </motion.div>

        {/* Middle divider / visual separation */}
        <div className="hidden lg:flex absolute left-1/2 top-1/3 bottom-1/3 -translate-x-1/2 flex-col items-center justify-center pointer-events-none">
          <div className="h-20 w-px bg-gradient-to-b from-transparent via-primary-500/20 to-transparent" />
          <div className="h-2 w-2 rounded-full bg-primary-500/40" />
          <div className="h-20 w-px bg-gradient-to-b from-transparent via-primary-500/20 to-transparent" />
        </div>
      </div>
    </section>
  );
}
