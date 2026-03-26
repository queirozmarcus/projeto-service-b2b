'use client';

import { useEffect, useRef, useState } from 'react';

interface Testimonial {
  quote: string;
  author: string;
  role: string;
  company: string;
  initials: string;
  color: string;
}

interface SocialProofProps {
  testimonials?: Testimonial[];
}

const DEFAULT_TESTIMONIALS: Testimonial[] = [
  {
    quote:
      'ScopeFlow cut our proposal time in half. The AI actually understands what clients mean, even when they ramble.',
    author: 'Marina Silva',
    role: 'Founder',
    company: 'MarinaCo Design',
    initials: 'MS',
    color: 'bg-primary-600',
  },
  {
    quote:
      'No more back-and-forth emails about scope creep. Clients see exactly what they agreed to, every time.',
    author: 'João Pedro',
    role: 'Digital Strategist',
    company: 'Agência JPM',
    initials: 'JP',
    color: 'bg-ink-700',
  },
  {
    quote:
      "The approval tracking is gold. Clients can't dispute an agreement that's signed and timestamped.",
    author: 'Ana Mendes',
    role: 'Project Manager',
    company: 'Landing Page House',
    initials: 'AM',
    color: 'bg-emerald-600',
  },
];

function TestimonialCard({
  testimonial,
  delay,
  inView,
}: {
  testimonial: Testimonial;
  delay: number;
  inView: boolean;
}) {
  return (
    <div
      className={`flex flex-col gap-5 rounded-3xl border border-secondary-200 bg-surface p-7 shadow-sm transition-all duration-700 ${
        inView ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-6'
      }`}
      style={{ transitionDelay: `${delay}ms` }}
    >
      {/* Large quote mark */}
      <span
        aria-hidden
        className="font-display text-6xl font-black leading-none text-primary-200 select-none"
      >
        &ldquo;
      </span>

      {/* Quote */}
      <p className="flex-1 text-base leading-relaxed text-ink-700">
        {testimonial.quote}
      </p>

      {/* Author */}
      <div className="flex items-center gap-3 border-t border-secondary-100 pt-5">
        <div
          className={`flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full text-sm font-bold text-white ${testimonial.color}`}
        >
          {testimonial.initials}
        </div>
        <div>
          <p className="text-sm font-semibold text-ink-800">{testimonial.author}</p>
          <p className="text-xs text-secondary-500">
            {testimonial.role} · {testimonial.company}
          </p>
        </div>
      </div>
    </div>
  );
}

export function SocialProof({ testimonials }: SocialProofProps) {
  const [inView, setInView] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const obs = new IntersectionObserver(
      ([e]) => { if (e.isIntersecting) setInView(true); },
      { threshold: 0.1 }
    );
    if (ref.current) obs.observe(ref.current);
    return () => obs.disconnect();
  }, []);

  const items = testimonials ?? DEFAULT_TESTIMONIALS;

  return (
    <section id="social-proof" className="bg-canvas px-6 py-20 lg:py-28">
      <div ref={ref} className="mx-auto max-w-6xl">

        {/* Header */}
        <div
          className={`mb-14 max-w-xl transition-all duration-700 ${
            inView ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-6'
          }`}
        >
          <p className="section-label mb-4">Testimonials</p>
          <h2 className="font-display text-4xl font-black leading-tight text-ink-900 lg:text-5xl">
            Loved by freelancers & agencies
          </h2>
        </div>

        {/* Cards */}
        <div className="grid gap-6 md:grid-cols-3">
          {items.map((t, i) => (
            <TestimonialCard
              key={i}
              testimonial={t}
              delay={i * 120}
              inView={inView}
            />
          ))}
        </div>

        {/* Trust strip */}
        <div
          className={`mt-16 border-t border-secondary-200 pt-10 transition-all duration-700 delay-300 ${
            inView ? 'opacity-100' : 'opacity-0'
          }`}
        >
          <p className="section-label mb-6 text-center">
            Trusted by 500+ teams across Brazil
          </p>
          <div className="flex flex-wrap items-center justify-center gap-6">
            {[
              'Freelancers',
              'Design Agencies',
              'Social Media Experts',
              'Web Developers',
              'Content Creators',
            ].map((label) => (
              <span
                key={label}
                className="rounded-full border border-secondary-200 bg-surface px-4 py-1.5 text-sm font-medium text-secondary-600"
              >
                {label}
              </span>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
