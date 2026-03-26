'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';

export function LandingNavbar() {
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 12);
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  return (
    <nav
      className={`sticky top-0 z-50 bg-canvas transition-all duration-300 ${
        scrolled
          ? 'shadow-sm border-b border-secondary-200/80 backdrop-blur-sm'
          : 'border-b border-transparent'
      }`}
    >
      <div className="mx-auto max-w-6xl px-6 py-4">
        <div className="flex items-center justify-between">

          {/* Logo */}
          <Link href="/" className="flex items-center gap-2.5">
            {/* Mark: stylized S */}
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary-600 shadow-sm">
              <svg className="h-4 w-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2.5}
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"
                />
              </svg>
            </div>
            <span className="text-[17px] font-bold tracking-tight text-ink-900">
              ScopeFlow
            </span>
          </Link>

          {/* Nav links — center */}
          <div className="hidden items-center gap-7 md:flex">
            {[
              { href: '#features', label: 'Features' },
              { href: '#pricing', label: 'Pricing' },
              { href: '#social-proof', label: 'Testimonials' },
            ].map(({ href, label }) => (
              <Link
                key={href}
                href={href}
                className="text-sm font-medium text-secondary-600 transition-colors hover:text-ink-900"
              >
                {label}
              </Link>
            ))}
          </div>

          {/* Auth */}
          <div className="flex items-center gap-3">
            <Link
              href="/auth/login"
              className="hidden rounded-lg px-4 py-2 text-sm font-medium text-secondary-700 transition-colors hover:text-ink-900 sm:block"
            >
              Log in
            </Link>
            <Link
              href="/auth/register"
              className="rounded-lg bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-700 hover:shadow"
            >
              Get Started
            </Link>
          </div>
        </div>
      </div>
    </nav>
  );
}
