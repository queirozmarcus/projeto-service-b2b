'use client';

import { useEffect, useRef, useState } from 'react';
import { PricingCard } from './PricingCard';

interface Plan {
  name: string;
  price: string;
  description?: string;
  features: string[];
  highlight?: boolean;
}

interface PricingTableProps {
  plans: Plan[];
  title?: string;
  subtitle?: string;
}

export function PricingTable({
  plans,
  title = 'Simple, Transparent Pricing',
  subtitle = 'Start free. Upgrade as you grow.',
}: PricingTableProps) {
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

  return (
    <section id="pricing" className="bg-surface-raised px-6 py-20 lg:py-28">
      <div ref={ref} className="mx-auto max-w-6xl">

        {/* Header */}
        <div
          className={`mb-16 text-center transition-all duration-700 ${
            inView ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-6'
          }`}
        >
          <p className="section-label mb-4">Pricing</p>
          <h2 className="font-display text-4xl font-black leading-tight text-ink-900 lg:text-5xl">
            {title}
          </h2>
          {subtitle && (
            <p className="mt-4 text-lg text-secondary-600">{subtitle}</p>
          )}
        </div>

        {/*
          Cards — highlighted card sits flush (no margin-top offset) and
          is slightly taller due to its internal padding. Non-highlighted
          cards get a small top margin to align vertically with the highlight card's content.
        */}
        <div className="grid gap-6 md:grid-cols-3 md:items-center">
          {plans.map((plan, index) => (
            <div
              key={index}
              className={`transition-all duration-700 ${
                inView ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
              } ${plan.highlight ? '' : 'md:mt-8'}`}
              style={{ transitionDelay: inView ? `${index * 100}ms` : '0ms' }}
            >
              <PricingCard
                name={plan.name}
                price={plan.price}
                description={plan.description}
                features={plan.features}
                cta={plan.price === 'Contact us' ? 'Talk to Sales' : 'Get Started Free'}
                ctaHref={plan.price === 'Contact us' ? '#' : '/auth/register'}
                highlight={plan.highlight ?? false}
              />
            </div>
          ))}
        </div>

        <p className="mt-10 text-center text-sm text-secondary-400">
          No credit card required. Cancel anytime.
        </p>
      </div>
    </section>
  );
}
