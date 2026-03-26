'use client';

import { useEffect, useRef, useState } from 'react';

interface Feature {
  icon?: string;
  title: string;
  description: string;
}

interface FeatureGridProps {
  features: Feature[];
  title?: string;
  subtitle?: string;
}

// ─── Inline SVG illustrations (no external images needed) ────────────────────

function DiscoveryIllustration() {
  return (
    <div className="flex h-full min-h-[220px] items-center justify-center rounded-2xl bg-primary-50 p-8">
      <div className="w-full max-w-xs space-y-3">
        {[
          { q: 'What is the main goal of this project?', w: '100%' },
          { q: 'Who is the target audience?', w: '80%' },
          { q: 'Do you have a deadline in mind?', w: '90%' },
        ].map((item, i) => (
          <div
            key={i}
            className="flex items-start gap-3 rounded-xl border border-primary-100 bg-surface p-3 shadow-xs"
          >
            <span className="mt-0.5 flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full bg-primary-600 text-xs font-bold text-white">
              {i + 1}
            </span>
            <p className="text-xs leading-relaxed text-ink-700">{item.q}</p>
          </div>
        ))}
        {/* Simulated typing indicator */}
        <div className="flex items-center gap-2 pl-1">
          <div className="flex gap-1">
            {[0, 1, 2].map((d) => (
              <span
                key={d}
                className="h-1.5 w-1.5 animate-pulse rounded-full bg-primary-400"
                style={{ animationDelay: `${d * 150}ms` }}
              />
            ))}
          </div>
          <span className="text-xs text-secondary-400">AI is generating next question...</span>
        </div>
      </div>
    </div>
  );
}

function ScopeIllustration() {
  return (
    <div className="flex h-full min-h-[220px] items-center justify-center rounded-2xl bg-secondary-50 p-8">
      <div className="w-full max-w-xs space-y-2">
        {/* Document-like card */}
        <div className="rounded-2xl border border-secondary-200 bg-surface p-4 shadow-sm">
          <div className="mb-3 flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-widest text-secondary-400">
              Scope Document
            </span>
            <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-semibold text-emerald-700">
              Generated
            </span>
          </div>
          {[
            { label: 'Deliverables', items: ['Landing page', 'Mobile-responsive', 'CMS integration'] },
            { label: 'Exclusions', items: ['Custom illustrations', 'SEO setup'] },
          ].map((section) => (
            <div key={section.label} className="mt-3">
              <p className="text-xs font-semibold text-ink-700">{section.label}</p>
              <ul className="mt-1 space-y-0.5">
                {section.items.map((item) => (
                  <li key={item} className="flex items-center gap-1.5 text-xs text-secondary-600">
                    <span className="h-1 w-1 rounded-full bg-secondary-400" />
                    {item}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function ApprovalIllustration() {
  return (
    <div className="flex h-full min-h-[220px] items-center justify-center rounded-2xl bg-emerald-50 p-8">
      <div className="w-full max-w-xs space-y-3">
        <div className="rounded-2xl border border-emerald-200 bg-surface p-5 text-center shadow-sm">
          <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-emerald-500">
            <svg className="h-6 w-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <p className="text-sm font-bold text-ink-800">Scope Approved</p>
          <p className="mt-1 text-xs text-secondary-500">by Marina Silva</p>
          <div className="mt-3 space-y-1.5 text-left">
            {[
              { label: 'Signed on', value: 'Mar 24, 2025' },
              { label: 'IP Address', value: '187.45.xx.xx' },
              { label: 'Version', value: 'v3 (final)' },
            ].map((r) => (
              <div key={r.label} className="flex justify-between text-xs">
                <span className="text-secondary-500">{r.label}</span>
                <span className="font-medium text-ink-700">{r.value}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

const ILLUSTRATIONS = [
  <DiscoveryIllustration key="discovery" />,
  <ScopeIllustration key="scope" />,
  <ApprovalIllustration key="approval" />,
];

// ─── Single feature row ───────────────────────────────────────────────────────
function FeatureRow({
  feature,
  index,
  illustration,
}: {
  feature: Feature;
  index: number;
  illustration: React.ReactNode;
}) {
  const [inView, setInView] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const obs = new IntersectionObserver(
      ([e]) => { if (e.isIntersecting) setInView(true); },
      { threshold: 0.15 }
    );
    if (ref.current) obs.observe(ref.current);
    return () => obs.disconnect();
  }, []);

  const reversed = index % 2 !== 0;

  return (
    <div
      ref={ref}
      className={`grid items-center gap-12 lg:grid-cols-2 transition-all duration-700 ${
        inView ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
      }`}
    >
      {/* Illustration — alternates left/right */}
      <div className={reversed ? 'lg:order-last' : ''}>
        {illustration}
      </div>

      {/* Text */}
      <div className="space-y-5">
        <span className="section-label">
          0{index + 1}
        </span>
        <h3 className="font-display text-3xl font-black leading-tight text-ink-900 lg:text-4xl">
          {feature.title}
        </h3>
        <p className="max-w-[40ch] text-base leading-relaxed text-secondary-600">
          {feature.description}
        </p>
      </div>
    </div>
  );
}

// ─── Section ─────────────────────────────────────────────────────────────────
export function FeatureGrid({
  features,
  title = 'Everything you need to close deals faster',
}: FeatureGridProps) {
  const displayFeatures = features.slice(0, 3);

  return (
    <section id="features" className="bg-canvas px-6 py-20 lg:py-28">
      <div className="mx-auto max-w-6xl">
        {/* Section header */}
        <div className="mb-20 max-w-xl">
          <p className="section-label mb-4">How It Works</p>
          <h2 className="font-display text-4xl font-black leading-tight text-ink-900 lg:text-5xl">
            {title}
          </h2>
        </div>

        {/* Feature rows */}
        <div className="space-y-24">
          {displayFeatures.map((feature, index) => (
            <FeatureRow
              key={index}
              feature={feature}
              index={index}
              illustration={ILLUSTRATIONS[index % ILLUSTRATIONS.length]}
            />
          ))}
        </div>
      </div>
    </section>
  );
}
