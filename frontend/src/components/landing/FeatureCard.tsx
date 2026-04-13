'use client';

import { motion } from 'framer-motion';

interface FeatureCardProps {
  icon: string | React.ReactNode;
  title: string;
  description: string;
}

export function FeatureCard({ icon, title, description }: FeatureCardProps) {
  return (
    <motion.div
      variants={{
        hidden: { opacity: 0, y: 24 },
        visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: [0.22, 1, 0.36, 1] } },
      }}
      className="group relative rounded-xl border border-dark-border bg-dark-surface/40 backdrop-blur-sm p-6 transition-all duration-300 hover:border-primary-500/40 hover:bg-dark-surface/80 hover:shadow-[0_0_24px_rgba(245,166,35,0.08)]"
    >
      {/* Top accent line reveals on hover */}
      <div className="absolute top-0 left-6 right-6 h-px bg-gradient-to-r from-transparent via-primary-500/40 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />

      {/* Corner accent badge */}
      <div className="absolute top-3 right-3 h-8 w-8 border border-primary-500/0 group-hover:border-primary-500/20 rounded-lg transition-colors duration-300" />

      <div className="relative space-y-4">
        {/* Icon with interaction */}
        <motion.div
          whileHover={{ scale: 1.12, rotate: 5 }}
          className="inline-flex h-14 w-14 items-center justify-center rounded-lg border border-primary-500/20 bg-primary-500/8 text-2xl font-bold text-primary-400 transition-all duration-300 group-hover:border-primary-500/40 group-hover:bg-primary-500/15"
        >
          {icon}
        </motion.div>

        {/* Content */}
        <div>
          <h3 className="text-base font-bold text-white/90 mb-2 leading-tight group-hover:text-white transition-colors">
            {title}
          </h3>
          <p className="text-sm leading-relaxed text-white/50 font-medium">
            {description}
          </p>
        </div>

        {/* Bottom divider — reveals with detail on hover */}
        <div className="pt-2 mt-4 border-t border-dark-border opacity-0 group-hover:opacity-100 transition-opacity duration-300">
          <p className="text-[11px] font-semibold uppercase tracking-widest text-primary-500/40">
            Saiba mais →
          </p>
        </div>
      </div>
    </motion.div>
  );
}
