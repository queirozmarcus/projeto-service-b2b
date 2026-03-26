'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { ArrowRightIcon } from '@heroicons/react/24/outline';
import { CheckCircleIcon } from '@heroicons/react/24/solid';

// ─── Product mockup: a clean flow diagram rendered in SVG-like HTML ──────────
function ProductMockup() {
  const steps = [
    {
      step: '01',
      title: 'AI Discovery',
      desc: 'Guided questions reveal the real scope',
      color: 'border-primary-200 bg-primary-50',
      badge: 'bg-primary-600',
    },
    {
      step: '02',
      title: 'Structured Briefing',
      desc: 'Answers consolidated into clear deliverables',
      color: 'border-secondary-200 bg-surface-raised',
      badge: 'bg-ink-700',
    },
    {
      step: '03',
      title: 'Approved Scope',
      desc: 'Client signs. Timestamp recorded. No disputes.',
      color: 'border-emerald-200 bg-emerald-50',
      badge: 'bg-emerald-600',
    },
  ];

  return (
    <div className="relative">
      {/* Offset shadow card */}
      <div className="absolute inset-0 translate-x-4 translate-y-4 rounded-3xl bg-primary-100/60" />

      {/* Main card */}
      <div className="relative rounded-3xl border border-secondary-200 bg-surface p-7 shadow-lg">
        {/* Window chrome */}
        <div className="mb-6 flex items-center gap-2 border-b border-secondary-100 pb-4">
          <span className="h-2.5 w-2.5 rounded-full bg-red-400" />
          <span className="h-2.5 w-2.5 rounded-full bg-yellow-400" />
          <span className="h-2.5 w-2.5 rounded-full bg-green-400" />
          <span className="ml-3 text-xs font-semibold tracking-wide text-secondary-400">
            ScopeFlow — Active Project
          </span>
        </div>

        {/* Steps */}
        <div className="space-y-3">
          {steps.map((s, i) => (
            <div
              key={i}
              className={`flex items-start gap-4 rounded-2xl border p-4 ${s.color}`}
            >
              <span
                className={`mt-0.5 flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full ${s.badge} text-xs font-bold text-white`}
              >
                {s.step}
              </span>
              <div>
                <p className="text-sm font-semibold text-ink-800">{s.title}</p>
                <p className="text-xs leading-relaxed text-secondary-500">{s.desc}</p>
              </div>
              {i === 2 && (
                <CheckCircleIcon className="ml-auto h-5 w-5 flex-shrink-0 text-emerald-500" />
              )}
            </div>
          ))}
        </div>

        {/* Metrics strip */}
        <div className="mt-5 grid grid-cols-3 gap-3 border-t border-secondary-100 pt-5">
          {[
            { value: '60%', label: 'Faster close' },
            { value: '92%', label: 'Approval rate' },
            { value: '4.8★', label: 'Avg rating' },
          ].map((m) => (
            <div key={m.label} className="text-center">
              <p className="text-lg font-black text-ink-800">{m.value}</p>
              <p className="text-xs text-secondary-400">{m.label}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── Hero ──────────────────────────────────────────────────────────────────────
export function Hero() {
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  if (!mounted) return null;

  return (
    <section className="relative overflow-hidden bg-canvas px-6 pb-28 pt-20">
      {/*
        Background: soft blue gradient blob — gives the section warmth without
        being distracting. Positioned top-right behind the mockup.
      */}
      <div
        aria-hidden
        className="pointer-events-none absolute -right-64 -top-64 h-[700px] w-[700px] rounded-full bg-primary-100/50 blur-3xl"
      />
      <div
        aria-hidden
        className="pointer-events-none absolute -left-32 bottom-0 h-96 w-96 rounded-full bg-secondary-200/40 blur-2xl"
      />

      <div className="relative mx-auto max-w-6xl">
        <div className="grid items-center gap-16 lg:grid-cols-[1fr_440px]">

          {/* ── Left: Headline + CTA ─────────────────────────────────── */}
          <div className="space-y-8">
            {/* Section label */}
            <p className="section-label">
              AI-Powered Scope Management
            </p>

            {/* Headline — Playfair, weight 900, generous size */}
            <h1 className="font-display text-[52px] font-black leading-[1.08] tracking-tight text-ink-900 lg:text-[64px] xl:text-[72px]">
              Turn Briefings into{' '}
              <em className="not-italic text-primary-600">Approved</em>{' '}
              Scopes
            </h1>

            {/* Subheadline */}
            <p className="max-w-[44ch] text-lg leading-relaxed text-secondary-600">
              Guide clients through AI-driven discovery. Get structured scopes,
              traceable approvals, and zero rework — in one workflow.
            </p>

            {/* Primary CTA — single, strong */}
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
              <Link
                href="/auth/register"
                className="group inline-flex items-center gap-2.5 rounded-xl bg-primary-600 px-8 py-4 text-base font-bold text-white shadow-md transition-all duration-200 hover:-translate-y-0.5 hover:bg-primary-700 hover:shadow-lg active:translate-y-0"
              >
                Start Free — No Card Needed
                <ArrowRightIcon className="h-4 w-4 transition-transform duration-200 group-hover:translate-x-1" />
              </Link>
              <p className="text-sm text-secondary-500">14-day trial · Cancel anytime</p>
            </div>

            {/* Social trust */}
            <div className="flex items-center gap-3 pt-2">
              <div className="flex -space-x-2.5">
                {['M', 'J', 'A', 'R'].map((letter) => (
                  <div
                    key={letter}
                    className="flex h-8 w-8 items-center justify-center rounded-full border-2 border-canvas bg-primary-600 text-xs font-bold text-white"
                  >
                    {letter}
                  </div>
                ))}
              </div>
              <p className="text-sm text-secondary-600">
                Trusted by <span className="font-semibold text-ink-800">500+</span> freelancers & agencies
              </p>
            </div>
          </div>

          {/* ── Right: Product mockup ─────────────────────────────────── */}
          <div>
            <ProductMockup />
          </div>
        </div>
      </div>
    </section>
  );
}
