'use client';

import { motion } from 'framer-motion';
import { PricingCard } from './PricingCard';

interface Plan {
  name: string;
  price: string;
  description: string;
  features: string[];
  highlight?: boolean;
}

interface PricingTableProps {
  plans: Plan[];
}

export function PricingTable({ plans }: PricingTableProps) {
  return (
    <section id="pricing" className="relative overflow-hidden bg-void px-6 py-32 border-t border-dark-border">
      {/* Background accents */}
      <div className="pointer-events-none absolute inset-0">
        <div className="absolute left-0 bottom-0 h-[500px] w-[500px] -translate-x-1/3 rounded-full bg-primary-500/4 blur-[140px]" />
        <div className="absolute right-0 top-1/3 h-96 w-96 rounded-full bg-secondary-500/3 blur-[120px]" />
      </div>

      <div className="relative mx-auto max-w-7xl">
        {/* Header */}
        <div className="mb-20 space-y-6 max-w-3xl">
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="flex items-center gap-3"
          >
            <span className="h-px w-8 bg-primary-500/40" />
            <span className="text-[11px] font-bold uppercase tracking-[0.22em] text-primary-500/70">
              Planos
            </span>
          </motion.div>

          <motion.h2
            initial={{ opacity: 0, y: 16 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.05 }}
            className="max-w-2xl font-display text-5xl font-black leading-tight text-white sm:text-6xl"
          >
            Preço direto,{' '}
            <span className="font-display italic text-primary-400">posicionamento premium.</span>
          </motion.h2>

          <motion.p
            initial={{ opacity: 0, y: 12 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.1 }}
            className="max-w-2xl text-base leading-relaxed text-white/40 font-medium"
          >
            Comece pequeno, padronize a operação e evolua para um fluxo comercial mais
            maduro sem trocar de ferramenta.
          </motion.p>
        </div>

        {/* Cards Grid */}
        <motion.div
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true, margin: '-80px' }}
          variants={{
            hidden: {},
            visible: { transition: { staggerChildren: 0.12, delayChildren: 0.1 } },
          }}
          className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3"
        >
          {plans.map((plan, i) => (
            <PricingCard key={i} {...plan} />
          ))}
        </motion.div>

        {/* CTA section */}
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ delay: 0.4 }}
          className="mt-20 rounded-xl border border-primary-500/20 bg-primary-500/5 p-8 text-center backdrop-blur-sm"
        >
          <p className="text-sm font-medium text-white/70 mb-2">
            Todos os planos incluem <span className="text-primary-400 font-semibold">14 dias grátis</span>
          </p>
          <p className="text-xs text-white/40">
            Sem cartão de crédito no início · Acesso completo · Cancele a qualquer momento
          </p>
        </motion.div>
      </div>
    </section>
  );
}
