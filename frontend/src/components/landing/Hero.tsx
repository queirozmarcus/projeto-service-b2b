'use client';

import Link from 'next/link';
import { motion } from 'framer-motion';
import { useState } from 'react';
import { ArrowRightIcon, SparklesIcon, CheckCircleIcon } from '@heroicons/react/24/outline';

function ProposalMockup() {
  return (
    <motion.div
      initial={{ opacity: 0, y: 32 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.8, delay: 0.3, ease: [0.22, 1, 0.36, 1] }}
      className="relative"
    >
      {/* Ambient glow */}
      <div className="absolute -inset-8 bg-primary-500/8 blur-3xl rounded-full pointer-events-none animate-ambient-pulse" />

      {/* Outer card */}
      <div className="relative rounded-2xl border border-dark-border bg-dark-surface shadow-2xl overflow-hidden">
        {/* Title bar */}
        <div className="flex items-center justify-between border-b border-dark-border px-5 py-3.5">
          <div className="flex items-center gap-2">
            <span className="h-2.5 w-2.5 rounded-full bg-red-500/60" />
            <span className="h-2.5 w-2.5 rounded-full bg-amber-500/60" />
            <span className="h-2.5 w-2.5 rounded-full bg-emerald-500/60" />
          </div>
          <span className="text-[10px] font-semibold tracking-[0.2em] text-white/20 uppercase">
            Proposta #024
          </span>
          <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-500/30 bg-amber-500/10 px-2.5 py-0.5 text-[10px] font-bold text-amber-400 uppercase tracking-wider">
            <span className="h-1.5 w-1.5 rounded-full bg-amber-400 animate-pulse" />
            Em Análise
          </span>
        </div>

        {/* Content */}
        <div className="p-5 space-y-5">
          {/* Client info */}
          <div>
            <p className="text-[11px] font-semibold text-white/30 uppercase tracking-widest mb-1">Cliente</p>
            <p className="text-base font-bold text-white">Acme Corp — Social Media Mensal</p>
            <p className="text-[12px] text-white/40 mt-0.5 font-medium">R$ 3.500/mês · Início em Abril 2026</p>
          </div>

          {/* Deliverables */}
          <div className="space-y-2">
            <p className="text-[11px] font-semibold text-white/30 uppercase tracking-widest">Entregáveis</p>
            {[
              '16 posts/mês com copy e arte',
              '4 Stories temáticos por semana',
              '2 Reels mensais com roteiro',
            ].map((item) => (
              <div key={item} className="flex items-center gap-2">
                <CheckCircleIcon className="h-3.5 w-3.5 flex-shrink-0 text-accent-500" />
                <span className="text-[12px] text-white/70 font-medium">{item}</span>
              </div>
            ))}
          </div>

          {/* Exclusions */}
          <div className="space-y-2 rounded-xl border border-dark-border bg-dark-raised p-3">
            <p className="text-[11px] font-semibold text-white/30 uppercase tracking-widest">Não inclui</p>
            <div className="flex flex-wrap gap-2">
              {['Gestão de anúncios', 'Fotografia', 'Edição de vídeo'].map((item) => (
                <span key={item} className="rounded-md border border-white/8 bg-white/5 px-2 py-0.5 text-[11px] font-medium text-white/40">
                  {item}
                </span>
              ))}
            </div>
          </div>

          {/* Action row */}
          <div className="flex items-center gap-3 border-t border-dark-border pt-4">
            <button className="flex-1 rounded-lg border border-dark-border py-2.5 text-[12px] font-semibold text-white/40 hover:border-dark-border-hover hover:text-white/60 transition-colors">
              Solicitar ajustes
            </button>
            <button className="flex-1 rounded-lg bg-primary-500 py-2.5 text-[12px] font-bold text-void shadow-[0_0_20px_rgba(245,166,35,0.3)] hover:bg-primary-400 transition-colors">
              ✓ Aprovar proposta
            </button>
          </div>
        </div>

        {/* Stats row */}
        <div className="border-t border-dark-border bg-dark-raised px-5 py-3 grid grid-cols-3 gap-4">
          {[
            { value: '2.1x', label: 'mais conversão' },
            { value: '24h', label: 'para aprovar' },
            { value: '0', label: 'idas e vindas' },
          ].map((m) => (
            <div key={m.label} className="text-center">
              <p className="text-sm font-black text-primary-400">{m.value}</p>
              <p className="text-[10px] font-semibold text-white/25 uppercase tracking-tight">{m.label}</p>
            </div>
          ))}
        </div>
      </div>
    </motion.div>
  );
}

function WaitlistForm() {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (email) setSubmitted(true);
  };

  if (submitted) {
    return (
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="flex items-center gap-3 rounded-xl border border-accent-500/30 bg-accent-500/10 px-5 py-3.5"
      >
        <CheckCircleIcon className="h-5 w-5 text-accent-400 flex-shrink-0" />
        <p className="text-sm font-semibold text-accent-300">
          Ótimo! Você está na lista. Avisaremos em breve.
        </p>
      </motion.div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="flex gap-2">
      <input
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="seu@email.com"
        required
        className="flex-1 rounded-xl border border-dark-border bg-dark-surface px-4 py-3 text-sm font-medium text-white placeholder-white/25 outline-none focus:border-primary-500/50 focus:ring-1 focus:ring-primary-500/20 transition-all"
      />
      <button
        type="submit"
        className="inline-flex items-center gap-2 rounded-xl bg-primary-500 hover:bg-primary-400 px-6 py-3 text-sm font-bold text-void shadow-[0_0_24px_rgba(245,166,35,0.35)] hover:shadow-[0_0_32px_rgba(245,166,35,0.5)] transition-all"
      >
        Entrar na lista
        <ArrowRightIcon className="h-4 w-4" />
      </button>
    </form>
  );
}

export function Hero() {
  return (
    <section className="relative flex min-h-screen items-center overflow-hidden bg-void px-6 pb-24 pt-32">
      {/* Background elements */}
      <div className="pointer-events-none absolute inset-0">
        {/* Amber top glow */}
        <div className="absolute -top-40 left-1/2 h-[600px] w-[800px] -translate-x-1/2 rounded-full bg-primary-500/6 blur-[120px]" />
        {/* Fine grid */}
        <div className="absolute inset-0 bg-[linear-gradient(to_right,#FFFFFF06_1px,transparent_1px),linear-gradient(to_bottom,#FFFFFF06_1px,transparent_1px)] bg-[size:72px_72px]" />
        {/* Radial fade at edges */}
        <div className="absolute inset-0 [mask-image:radial-gradient(ellipse_80%_60%_at_50%_0%,#000_60%,transparent_100%)]" />
      </div>

      <div className="relative mx-auto max-w-7xl w-full">
        <div className="grid items-center gap-16 lg:grid-cols-[1fr_0.85fr] xl:gap-24">

          {/* Left column — copy */}
          <div className="space-y-8">
            {/* Badge */}
            <motion.div
              initial={{ opacity: 0, y: -12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5 }}
              className="inline-flex items-center gap-2 rounded-full border border-primary-500/25 bg-primary-500/8 px-4 py-2 text-[11px] font-bold uppercase tracking-[0.2em] text-primary-400"
            >
              <SparklesIcon className="h-3.5 w-3.5" />
              Briefing comercial com inteligência aplicada
            </motion.div>

            {/* Headline */}
            <motion.h1
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.7, delay: 0.08, ease: [0.22, 1, 0.36, 1] }}
              className="font-display text-6xl font-black leading-[0.95] tracking-tight text-white sm:text-7xl lg:text-8xl"
            >
              Feche projetos com um escopo{' '}
              <span className="relative">
                <span className="font-display italic text-primary-400">claro, bonito</span>
                <motion.span
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: 1.2, duration: 0.6 }}
                  className="absolute inset-0 bg-gradient-to-r from-primary-400 via-primary-300 to-primary-400 bg-[length:200%_100%] opacity-20 blur-xl -z-10 rounded-lg"
                />
              </span>{' '}
              <span className="text-white/80">e aprovado.</span>
            </motion.h1>

            {/* Subtext */}
            <motion.p
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: 0.2 }}
              className="max-w-[44ch] text-lg leading-relaxed text-white/50 font-medium"
            >
              Troque mensagens soltas, PDFs improvisados e retrabalho por uma jornada
              elegante de descoberta, alinhamento e aprovação em minutos.
            </motion.p>

            {/* Trust signals */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.28 }}
              className="flex flex-wrap gap-2"
            >
              {[
                'Diagnóstico guiado por IA',
                'Escopo com linguagem executiva',
                'Aprovação com rastreabilidade',
              ].map((item) => (
                <span
                  key={item}
                  className="inline-flex items-center gap-1.5 rounded-lg border border-white/8 bg-white/4 px-3 py-1.5 text-[12px] font-medium text-white/55"
                >
                  <span className="h-1 w-1 rounded-full bg-accent-400" />
                  {item}
                </span>
              ))}
            </motion.div>

            {/* Waitlist form */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.35 }}
              className="space-y-3 max-w-[480px]"
            >
              <WaitlistForm />
              <p className="text-[11px] text-white/25 font-medium pl-1">
                Sem cartão de crédito. Acesso antecipado gratuito por 14 dias.
              </p>
            </motion.div>

            {/* CTA buttons */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.38 }}
              className="flex flex-wrap items-center gap-4"
            >
              <Link
                href="/auth/register"
                className="group inline-flex items-center gap-2 rounded-xl bg-white/8 border border-white/12 hover:bg-white/12 px-6 py-3 text-sm font-semibold text-white transition-all"
              >
                Criar conta agora
                <ArrowRightIcon className="h-4 w-4 transition-transform group-hover:translate-x-1" />
              </Link>
              <Link
                href="#how-it-works"
                className="text-sm font-medium text-white/35 hover:text-white/60 transition-colors"
              >
                Ver como funciona →
              </Link>
            </motion.div>

            {/* Social proof line */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ duration: 0.6, delay: 0.5 }}
              className="flex items-center gap-4"
            >
              <div className="flex -space-x-2.5">
                {['AC', 'RL', 'JP', 'MB', 'TF'].map((initials) => (
                  <div
                    key={initials}
                    className="flex h-8 w-8 items-center justify-center rounded-full border-2 border-void bg-dark-raised text-[9px] font-bold text-white/60"
                  >
                    {initials}
                  </div>
                ))}
              </div>
              <p className="text-[12px] font-medium text-white/35">
                <span className="text-white/60 font-bold">+180 equipes</span> já usam ScopeFlow
              </p>
            </motion.div>
          </div>

          {/* Right column — product mockup */}
          <div className="hidden lg:block">
            <ProposalMockup />
          </div>
        </div>
      </div>
    </section>
  );
}
