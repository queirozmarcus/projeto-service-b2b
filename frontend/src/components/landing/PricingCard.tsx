'use client';

import { motion } from 'framer-motion';
import Link from 'next/link';
import { CheckIcon } from '@heroicons/react/24/outline';

interface PricingCardProps {
  name: string;
  price: string;
  description: string;
  features: string[];
  highlight?: boolean;
}

export function PricingCard({ name, price, description, features, highlight }: PricingCardProps) {
  return (
    <motion.div
      variants={{
        hidden: { opacity: 0, y: 20 },
        visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: [0.22, 1, 0.36, 1] } },
      }}
      className="group relative"
    >
      <div
        className={`relative rounded-xl border p-8 transition-all duration-300 backdrop-blur-sm ${
          highlight
            ? 'border-primary-500/40 bg-dark-surface/80 shadow-[0_0_40px_rgba(245,166,35,0.12)]'
            : 'border-dark-border bg-dark-surface/40 hover:border-primary-500/20 hover:bg-dark-surface/60'
        }`}
      >
        {/* Top accent line for highlight card */}
        {highlight && (
          <div className="absolute top-0 left-8 right-8 h-px bg-gradient-to-r from-transparent via-primary-500/60 to-transparent" />
        )}

        {/* Corner accent */}
        <div className={`absolute top-3 right-3 h-8 w-8 border rounded-lg transition-colors ${
          highlight
            ? 'border-primary-500/30'
            : 'border-primary-500/0 group-hover:border-primary-500/20'
        }`} />

        {/* Highlight badge */}
        {highlight && (
          <motion.div
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            className="absolute -top-4 left-1/2 -translate-x-1/2"
          >
            <div className="rounded-full border border-primary-500/40 bg-dark-raised px-4 py-1.5 text-[10px] font-black uppercase tracking-widest text-primary-400">
              Mais escolhido
            </div>
          </motion.div>
        )}

        <div className="space-y-6">
          {/* Plan name + description */}
          <div>
            <h3 className="text-sm font-bold uppercase tracking-widest text-white/70 group-hover:text-white/90 transition-colors">
              {name}
            </h3>
            <p className="mt-2 text-[13px] text-white/40 font-medium leading-relaxed">
              {description}
            </p>
          </div>

          {/* Price with visual emphasis */}
          <div className="flex items-baseline gap-2">
            <span
              className={`font-display text-5xl font-black transition-colors ${
                highlight ? 'text-primary-300' : 'text-white group-hover:text-primary-400/80'
              }`}
            >
              {price}
            </span>
            {price !== 'Sob consulta' && (
              <span className="text-[12px] font-semibold text-white/30 uppercase tracking-tight">
                /mês
              </span>
            )}
          </div>

          {/* CTA Button */}
          <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
            <Link
              href="/auth/register"
              className={`flex items-center justify-center rounded-lg py-3.5 text-sm font-bold transition-all ${
                highlight
                  ? 'bg-primary-500 text-void shadow-[0_0_24px_rgba(245,166,35,0.35)] hover:bg-primary-400 hover:shadow-[0_0_32px_rgba(245,166,35,0.5)]'
                  : 'border border-dark-border-hover bg-dark-surface/30 text-white/70 hover:bg-dark-surface hover:text-white/90 hover:border-primary-500/20'
              }`}
            >
              {highlight ? 'Testar agora' : 'Começar'}
            </Link>
          </motion.div>

          {/* Divider */}
          <div className="border-t border-dark-border opacity-50 group-hover:opacity-100 transition-opacity" />

          {/* Features list */}
          <ul className="space-y-3">
            {features.map((feature, i) => (
              <motion.li
                key={i}
                initial={{ opacity: 0, x: -4 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.05 }}
                className="flex items-start gap-2.5 text-[13px] font-medium text-white/55"
              >
                <CheckIcon
                  className={`mt-0.5 h-4 w-4 flex-shrink-0 transition-colors ${
                    highlight
                      ? 'text-primary-500'
                      : 'text-accent-500/60 group-hover:text-accent-500'
                  }`}
                />
                {feature}
              </motion.li>
            ))}
          </ul>
        </div>
      </div>
    </motion.div>
  );
}
