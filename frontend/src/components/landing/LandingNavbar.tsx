'use client';

import Link from 'next/link';
import { motion } from 'framer-motion';
import { useState, useEffect } from 'react';

export function LandingNavbar() {
  const [isScrolled, setIsScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => setIsScrolled(window.scrollY > 40);
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <motion.nav
      initial={{ y: -16, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-500 ${
        isScrolled
          ? 'bg-void/80 backdrop-blur-xl border-b border-dark-border'
          : 'bg-transparent'
      }`}
    >
      <div className="mx-auto max-w-7xl flex items-center justify-between px-6 py-4">
        {/* Logo */}
        <Link href="/" className="flex items-center gap-3 group">
          <div className="relative flex h-8 w-8 items-center justify-center">
            <div className="absolute inset-0 rounded-lg bg-primary-500/20 blur-md group-hover:bg-primary-500/30 transition-all" />
            <div className="relative flex h-8 w-8 items-center justify-center rounded-lg border border-primary-500/40 bg-primary-500/10 text-[10px] font-black text-primary-400 tracking-tight">
              SF
            </div>
          </div>
          <span className="font-display text-lg font-bold text-white tracking-tight">
            Scope<span className="text-primary-400">Flow</span>
          </span>
        </Link>

        {/* Nav links */}
        <div className="hidden md:flex items-center gap-8">
          {[
            ['Recursos', '#features'],
            ['Como Funciona', '#how-it-works'],
            ['Planos', '#pricing'],
            ['FAQ', '#faq'],
          ].map(([label, href]) => (
            <Link
              key={label}
              href={href}
              className="text-[13px] font-medium text-white/50 hover:text-white/90 transition-colors tracking-wide"
            >
              {label}
            </Link>
          ))}
        </div>

        {/* CTAs */}
        <div className="flex items-center gap-3">
          <Link
            href="/auth/login"
            className="hidden sm:block text-[13px] font-medium text-white/60 hover:text-white transition-colors"
          >
            Entrar
          </Link>
          <Link
            href="/auth/register"
            className="inline-flex items-center gap-2 rounded-lg border border-primary-500/40 bg-primary-500/10 hover:bg-primary-500/20 px-4 py-2 text-[13px] font-semibold text-primary-300 hover:text-primary-200 transition-all"
          >
            Começar grátis
          </Link>
        </div>
      </div>
    </motion.nav>
  );
}
