'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';

export function Hero() {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) return null;

  return (
    <section className="relative min-h-screen overflow-hidden bg-gradient-to-br from-primary-50 via-white to-secondary-50 px-6 py-20">
      {/* Background decorative elements */}
      <div className="absolute -right-32 -top-32 h-96 w-96 rounded-full bg-gradient-to-br from-primary-200 to-primary-50 opacity-40 blur-3xl" />
      <div className="absolute -bottom-32 -left-32 h-96 w-96 rounded-full bg-gradient-to-tr from-primary-100 to-transparent opacity-30 blur-3xl" />

      <div className="relative mx-auto max-w-6xl">
        <div className="grid items-center gap-12 md:grid-cols-2">
          {/* Left: Headline + CTA */}
          <div className="animate-slide-up space-y-8">
            {/* Eyebrow */}
            <div className="inline-block">
              <span className="inline-flex items-center gap-2 rounded-full bg-primary-100 px-4 py-2 text-sm font-medium text-primary-700">
                <span className="h-2 w-2 rounded-full bg-primary-600" />
                AI-Powered Discovery
              </span>
            </div>

            {/* Headline - Distinctive Typography */}
            <h1 className="font-display text-5xl font-800 leading-tight text-secondary-900 md:text-6xl lg:text-7xl">
              Transform <span className="bg-gradient-to-r from-primary-600 to-primary-500 bg-clip-text text-transparent">Briefings</span> into Approved Scopes
            </h1>

            {/* Subheadline */}
            <p className="text-lg leading-relaxed text-secondary-600 md:text-xl">
              Let AI guide your clients through structured discovery. Get clear agreements, faster approvals, and eliminate rework—all in one platform.
            </p>

            {/* CTA Buttons - Enhanced with hover states */}
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
              <Link
                href="/auth/register"
                className="group relative inline-flex items-center justify-center overflow-hidden rounded-xl bg-gradient-to-r from-primary-600 to-primary-500 px-8 py-4 font-semibold text-white shadow-lg transition-all duration-300 hover:shadow-xl hover:scale-105 active:scale-95"
              >
                <span className="relative z-10">Start Free Trial</span>
                <div className="absolute inset-0 -z-10 bg-gradient-to-r from-primary-700 to-primary-600 opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
              </Link>

              <Link
                href="#features"
                className="inline-flex items-center justify-center rounded-xl border-2 border-primary-600 px-8 py-4 font-semibold text-primary-600 transition-all duration-300 hover:bg-primary-50 hover:border-primary-700"
              >
                See How It Works
              </Link>
            </div>

            {/* Social Proof */}
            <div className="pt-6">
              <p className="text-sm font-medium text-secondary-600">
                ✨ Trusted by 500+ freelancers & agencies
              </p>
            </div>
          </div>

          {/* Right: Visual Element - Asymmetrical Design */}
          <div className="relative h-full min-h-96 animate-slide-down">
            {/* Glassmorphism card with stats */}
            <div className="absolute inset-0 flex items-center justify-center">
              <div className="relative space-y-6 rounded-2xl border border-white/30 bg-white/50 p-8 backdrop-blur-xl">
                {/* Stat 1 */}
                <div className="group cursor-pointer transition-all duration-300 hover:scale-105">
                  <div className="text-xs font-semibold uppercase tracking-widest text-primary-600">
                    Discovery Time
                  </div>
                  <p className="text-2xl font-bold text-secondary-900">
                    60% Faster
                  </p>
                  <p className="text-sm text-secondary-600">
                    AI-guided questions
                  </p>
                </div>

                {/* Divider */}
                <div className="h-px bg-gradient-to-r from-transparent via-primary-200 to-transparent" />

                {/* Stat 2 */}
                <div className="group cursor-pointer transition-all duration-300 hover:scale-105">
                  <div className="text-xs font-semibold uppercase tracking-widest text-primary-600">
                    Approval Rate
                  </div>
                  <p className="text-2xl font-bold text-secondary-900">
                    92% on First Try
                  </p>
                  <p className="text-sm text-secondary-600">
                    Clear, structured scope
                  </p>
                </div>

                {/* Divider */}
                <div className="h-px bg-gradient-to-r from-transparent via-primary-200 to-transparent" />

                {/* Stat 3 */}
                <div className="group cursor-pointer transition-all duration-300 hover:scale-105">
                  <div className="text-xs font-semibold uppercase tracking-widest text-primary-600">
                    Client Satisfaction
                  </div>
                  <p className="text-2xl font-bold text-secondary-900">
                    4.8/5 Stars
                  </p>
                  <p className="text-sm text-secondary-600">
                    From 500+ testimonials
                  </p>
                </div>
              </div>
            </div>

            {/* Floating accent elements */}
            <div className="absolute -bottom-6 -right-6 h-48 w-48 rounded-full bg-gradient-to-tl from-primary-300 to-transparent opacity-20 blur-2xl" />
            <div className="absolute -top-6 -left-6 h-40 w-40 rounded-full bg-gradient-to-br from-primary-200 to-transparent opacity-30 blur-2xl" />
          </div>
        </div>
      </div>

      {/* Scroll indicator */}
      <div className="absolute bottom-8 left-1/2 -translate-x-1/2 animate-bounce">
        <div className="flex flex-col items-center gap-2 text-secondary-600">
          <span className="text-xs font-medium uppercase tracking-widest">
            Scroll
          </span>
          <svg
            className="h-5 w-5"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M19 14l-7 7m0 0l-7-7m7 7V3"
            />
          </svg>
        </div>
      </div>
    </section>
  );
}
