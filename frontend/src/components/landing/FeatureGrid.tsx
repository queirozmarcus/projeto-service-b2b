'use client';

import { motion } from 'framer-motion';
import { FeatureCard } from './FeatureCard';

interface Feature {
  icon: string | React.ReactNode;
  title: string;
  description: string;
}

interface FeatureGridProps {
  features: Feature[];
}

export function FeatureGrid({ features }: FeatureGridProps) {
  return (
    <section id="features" className="relative overflow-hidden bg-pitch px-6 py-32 border-t border-dark-border">
      {/* Background accent */}
      <div className="pointer-events-none absolute inset-0">
        <div className="absolute right-1/4 top-1/2 h-[600px] w-[600px] -translate-y-1/2 rounded-full bg-primary-500/4 blur-[140px]" />
        {/* Subtle pattern */}
        <div className="absolute inset-0 bg-[linear-gradient(180deg,#FFFFFF04_0.5px,transparent_0.5px)] bg-[size:auto_64px]" />
      </div>

      <div className="relative mx-auto max-w-7xl">
        {/* Section header */}
        <div className="mb-20 space-y-6 max-w-3xl">
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="flex items-center gap-3"
          >
            <span className="h-px w-8 bg-primary-500/40" />
            <span className="text-[11px] font-bold uppercase tracking-[0.22em] text-primary-500/70">
              Recursos principais
            </span>
          </motion.div>

          <motion.h2
            initial={{ opacity: 0, y: 16 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.05 }}
            className="font-display text-5xl font-black leading-tight text-white sm:text-6xl"
          >
            Uma jornada mais sofisticada,{' '}
            <span className="font-display italic text-primary-400">
              do primeiro contato ao aceite.
            </span>
          </motion.h2>

          <motion.p
            initial={{ opacity: 0, y: 12 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.1 }}
            className="max-w-2xl text-base leading-relaxed text-white/40 font-medium"
          >
            Cada etapa foi projetada para transmitir confiança, reduzir ambiguidade e
            acelerar a decisão do cliente. Nada de genérico aqui.
          </motion.p>
        </div>

        {/* Cards Grid */}
        <motion.div
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true, margin: '-80px' }}
          variants={{
            hidden: {},
            visible: { transition: { staggerChildren: 0.1, delayChildren: 0.05 } },
          }}
          className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3"
        >
          {features.map((feature, i) => (
            <FeatureCard key={i} {...feature} />
          ))}
        </motion.div>

        {/* Bottom accent / divider */}
        <motion.div
          initial={{ opacity: 0, scaleX: 0 }}
          whileInView={{ opacity: 1, scaleX: 1 }}
          viewport={{ once: true }}
          transition={{ delay: 0.4, duration: 0.6 }}
          className="mt-16 h-px bg-gradient-to-r from-transparent via-primary-500/20 to-transparent origin-center"
        />
      </div>
    </section>
  );
}
