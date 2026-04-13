'use client';

import Link from 'next/link';

const NAV_LINKS = [
  {
    heading: 'Produto',
    links: [
      ['Recursos', '#features'],
      ['Como funciona', '#how-it-works'],
      ['Planos', '#pricing'],
      ['FAQ', '#faq'],
    ],
  },
  {
    heading: 'Para quem',
    links: [
      ['Agências', '#'],
      ['Consultorias', '#'],
      ['Software houses', '#'],
      ['Freelancers', '#'],
    ],
  },
  {
    heading: 'Conta',
    links: [
      ['Criar conta', '/auth/register'],
      ['Entrar', '/auth/login'],
      ['Plano Enterprise', '#pricing'],
      ['Contato', 'mailto:oi@scopeflow.app'],
    ],
  },
];

export function Footer() {
  return (
    <footer className="bg-void border-t border-dark-border px-6 py-20">
      <div className="mx-auto max-w-7xl">
        <div className="grid gap-12 sm:grid-cols-2 lg:grid-cols-[1.5fr_1fr_1fr_1fr]">
          {/* Brand */}
          <div className="space-y-5">
            <Link href="/" className="flex items-center gap-2.5 group">
              <div className="relative flex h-8 w-8 items-center justify-center">
                <div className="absolute inset-0 rounded-lg bg-primary-500/15 blur-sm group-hover:bg-primary-500/25 transition-all" />
                <div className="relative flex h-8 w-8 items-center justify-center rounded-lg border border-primary-500/30 bg-primary-500/8 text-[10px] font-black text-primary-400">
                  SF
                </div>
              </div>
              <span className="font-display text-lg font-bold text-white tracking-tight">
                Scope<span className="text-primary-400">Flow</span>
              </span>
            </Link>

            <p className="max-w-xs text-sm leading-relaxed text-white/30 font-medium">
              Plataforma para transformar conversa comercial em briefing estruturado,
              escopo elegante e aprovação sem atrito.
            </p>

            <div className="flex gap-4">
              {['LinkedIn', 'Instagram', 'GitHub'].map((social) => (
                <Link
                  key={social}
                  href="#"
                  className="text-[11px] font-semibold uppercase tracking-widest text-white/20 hover:text-primary-400 transition-colors"
                >
                  {social}
                </Link>
              ))}
            </div>
          </div>

          {/* Nav columns */}
          {NAV_LINKS.map(({ heading, links }) => (
            <div key={heading}>
              <h4 className="mb-5 text-[11px] font-black uppercase tracking-[0.2em] text-white/30">
                {heading}
              </h4>
              <ul className="space-y-3">
                {links.map(([label, href]) => (
                  <li key={label}>
                    <Link
                      href={href}
                      className="text-sm font-medium text-white/35 hover:text-white/65 transition-colors"
                    >
                      {label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        {/* Bottom bar */}
        <div className="mt-16 flex flex-col items-center justify-between gap-4 border-t border-dark-border pt-8 sm:flex-row">
          <p className="text-[11px] font-semibold uppercase tracking-widest text-white/20">
            © 2026 ScopeFlow AI. Todos os direitos reservados.
          </p>
          <div className="flex gap-6">
            {['Privacidade', 'Termos', 'LGPD'].map((item) => (
              <Link
                key={item}
                href="#"
                className="text-[11px] font-medium text-white/20 hover:text-white/40 transition-colors"
              >
                {item}
              </Link>
            ))}
          </div>
        </div>
      </div>
    </footer>
  );
}
