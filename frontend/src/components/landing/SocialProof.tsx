'use client';

import { motion } from 'framer-motion';

const TESTIMONIALS = [
  {
    quote:
      'Antes eu vendia projeto com notas soltas no WhatsApp. Agora eu apresento um escopo limpo, convincente e fácil de aprovar. Minha taxa de fechamento subiu 40%.',
    author: 'Marina Costa',
    role: 'Diretora de Contas',
    company: 'MC Agência',
    avatar: 'MC',
  },
  {
    quote:
      'A sensação mudou completamente: parece produto premium, não um formulário qualquer. Isso elevou a percepção da nossa operação na hora do pitch.',
    author: 'Rafael Lima',
    role: 'Founder',
    company: 'Consultoria Digital RL',
    avatar: 'RL',
  },
  {
    quote:
      'O cliente entende melhor o que está comprando e nossa equipe entra na execução com muito menos ruído. Economizamos horas por projeto.',
    author: 'Juliana Prado',
    role: 'Head de Projetos',
    company: 'Studio JP',
    avatar: 'JP',
  },
];

const STATS = [
  { value: '31%', label: 'menos idas e vindas comerciais' },
  { value: '3.2×', label: 'mais velocidade para aprovação' },
  { value: '87%', label: 'taxa média de aceite' },
];

export function SocialProof() {
  return (
    <section
      id="testimonials"
      className="relative overflow-hidden bg-pitch px-6 py-28 border-t border-dark-border"
    >
      <div className="pointer-events-none absolute inset-0">
        <div className="absolute right-0 top-0 h-[500px] w-[500px] rounded-full bg-primary-500/4 blur-[120px]" />
      </div>

      <div className="relative mx-auto max-w-7xl">
        <div className="grid gap-20 lg:grid-cols-[0.9fr_1.1fr] lg:items-start">

          {/* Left — headline + stats */}
          <div className="space-y-10 lg:sticky lg:top-24">
            <div className="space-y-4">
              <motion.div
                initial={{ opacity: 0, y: 12 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                className="flex items-center gap-3"
              >
                <span className="h-px w-8 bg-primary-500/40" />
                <span className="text-[11px] font-bold uppercase tracking-[0.22em] text-primary-500/70">
                  Resultados
                </span>
              </motion.div>

              <motion.h2
                initial={{ opacity: 0, y: 16 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: 0.05 }}
                className="font-display text-3xl font-black leading-tight text-white sm:text-4xl"
              >
                Aparência premium,{' '}
                <span className="font-display italic text-primary-400">
                  impacto comercial real.
                </span>
              </motion.h2>

              <motion.p
                initial={{ opacity: 0, y: 12 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: 0.1 }}
                className="max-w-xs text-base leading-relaxed text-white/40 font-medium"
              >
                Quando o processo parece produto premium, o cliente percebe mais valor
                antes mesmo de aprovar.
              </motion.p>
            </div>

            {/* Stats */}
            <div className="space-y-4">
              {STATS.map((stat, i) => (
                <motion.div
                  key={i}
                  initial={{ opacity: 0, x: -16 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: 0.1 + i * 0.08 }}
                  className="flex items-baseline gap-4 rounded-xl border border-dark-border bg-dark-surface px-5 py-4"
                >
                  <span className="font-display text-3xl font-black text-primary-400 tabular-nums">
                    {stat.value}
                  </span>
                  <span className="text-sm font-medium text-white/40 leading-tight">
                    {stat.label}
                  </span>
                </motion.div>
              ))}
            </div>

            {/* Segments */}
            <div className="flex flex-wrap gap-2">
              {['Agências', 'Consultorias', 'Software Houses', 'Freelancers'].map((item) => (
                <span
                  key={item}
                  className="rounded-full border border-dark-border bg-dark-raised px-3 py-1.5 text-[11px] font-semibold text-white/35"
                >
                  {item}
                </span>
              ))}
            </div>
          </div>

          {/* Right — testimonials */}
          <div className="space-y-4">
            {TESTIMONIALS.map((t, i) => (
              <motion.div
                key={i}
                initial={{ opacity: 0, x: 24 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1, duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
                className="group rounded-2xl border border-dark-border bg-dark-surface p-7 transition-all duration-300 hover:border-dark-border-hover"
              >
                {/* Quote mark */}
                <div className="mb-4 font-display text-5xl font-black leading-none text-primary-500/20 select-none">
                  &ldquo;
                </div>

                <p className="mb-6 text-[15px] leading-relaxed text-white/65 font-medium">
                  {t.quote}
                </p>

                <div className="flex items-center gap-3 border-t border-dark-border pt-5">
                  <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full border border-primary-500/20 bg-primary-500/10 text-[11px] font-black text-primary-400">
                    {t.avatar}
                  </div>
                  <div>
                    <p className="text-sm font-bold text-white/80">{t.author}</p>
                    <p className="text-[11px] font-medium text-white/30">
                      {t.role} · {t.company}
                    </p>
                  </div>
                  <div className="ml-auto flex gap-0.5">
                    {Array.from({ length: 5 }).map((_, j) => (
                      <span key={j} className="text-xs text-primary-500/70">★</span>
                    ))}
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
