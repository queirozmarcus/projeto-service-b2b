'use client';

import { motion } from 'framer-motion';
import Link from 'next/link';
import { ArrowRightIcon } from '@heroicons/react/24/outline';

export function CTASection() {
  return (
    <section className="relative overflow-hidden bg-pitch px-6 py-32 border-t border-dark-border">
      {/* Background accents */}
      <div className="pointer-events-none absolute inset-0">
        <div className="absolute left-1/2 top-1/2 h-[600px] w-[800px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-primary-500/6 blur-[120px]" />
        <div className="absolute -left-32 top-0 h-96 w-96 rounded-full bg-secondary-500/4 blur-[100px]" />
      </div>

      <div className="relative mx-auto max-w-7xl">
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
          className="relative overflow-hidden rounded-2xl border border-primary-500/25 bg-dark-surface/60 backdrop-blur px-8 py-20 text-center lg:px-20"
        >
          {/* Top border glow */}
          <div className="absolute top-0 left-12 right-12 h-px bg-gradient-to-r from-transparent via-primary-500/50 to-transparent" />

          {/* Subtle corner accents */}
          <div className="absolute top-6 left-6 h-10 w-10 border border-primary-500/15 rounded-lg" />
          <div className="absolute top-6 right-6 h-10 w-10 border border-primary-500/15 rounded-lg" />
          <div className="absolute bottom-6 left-6 h-10 w-10 border border-primary-500/15 rounded-lg" />
          <div className="absolute bottom-6 right-6 h-10 w-10 border border-primary-500/15 rounded-lg" />

          <div className="relative space-y-8">
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: 0.1 }}
              className="space-y-4"
            >
              <span className="inline-block text-[11px] font-bold uppercase tracking-widest text-primary-500/70">
                Comece hoje, use para sempre
              </span>
              <h2 className="font-display text-5xl font-black leading-tight text-white sm:text-6xl lg:text-7xl">
                Sua operação merece{' '}
                <span className="font-display italic text-primary-400">
                  um fluxo com cara de produto.
                </span>
              </h2>
            </motion.div>

            <motion.p
              initial={{ opacity: 0, y: 12 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: 0.15 }}
              className="mx-auto max-w-2xl text-base leading-relaxed text-white/40 font-medium"
            >
              Se a primeira impressão precisa vender confiança, a experiência de briefing precisa
              fechar a conta. Comece com uma base mais profissional hoje mesmo.
            </motion.p>

            <motion.div
              initial={{ opacity: 0, y: 12 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: 0.2 }}
              className="flex flex-col items-center justify-center gap-4 sm:flex-row"
            >
              <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
                <Link
                  href="/auth/register"
                  className="group inline-flex items-center gap-3 rounded-lg bg-primary-500 px-8 py-4 text-[15px] font-bold text-void shadow-[0_0_32px_rgba(245,166,35,0.35)] transition-all hover:bg-primary-400 hover:shadow-[0_0_48px_rgba(245,166,35,0.5)]"
                >
                  Criar conta gratuita
                  <ArrowRightIcon className="h-4 w-4 transition-transform group-hover:translate-x-1" />
                </Link>
              </motion.div>
              <Link
                href="#pricing"
                className="text-sm font-medium text-white/35 hover:text-white/60 transition-colors"
              >
                Ver planos →
              </Link>
            </motion.div>

            <p className="text-[12px] font-medium text-white/20">
              14 dias grátis · Sem cartão de crédito · Cancele quando quiser
            </p>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
