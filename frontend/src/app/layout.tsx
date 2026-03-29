import type { Metadata } from 'next';
import { Fraunces, DM_Sans } from 'next/font/google';
import '@/styles/globals.css';
import { SessionProvider } from '@/components/auth/SessionProvider';

const fraunces = Fraunces({
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-display',
  weight: ['300', '400', '600', '700', '800', '900'],
  style: ['normal', 'italic'],
});

const dmSans = DM_Sans({
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-sans',
  weight: ['300', '400', '500', '600', '700'],
});

export const metadata: Metadata = {
  title: 'ScopeFlow — Briefing inteligente, escopos aprovados',
  description:
    'Transforme conversas vagas em escopos claros, aprovados e rastreáveis com uma experiência de briefing orientada por IA.',
  keywords: 'briefing com IA, gestao de escopo, aprovacao de projeto, saas b2b, freelancers, agencias',
  authors: [{ name: 'ScopeFlow' }],
  robots: 'index, follow',
  openGraph: {
    type: 'website',
    locale: 'pt_BR',
    url: 'https://scopeflow.app',
    siteName: 'ScopeFlow',
    title: 'ScopeFlow — Briefing inteligente, escopos aprovados',
    description: 'Transforme conversas vagas em escopos claros, aprovados e rastreáveis com IA.',
    images: [
      {
        url: 'https://scopeflow.app/og-image.png',
        width: 1200,
        height: 630,
        alt: 'ScopeFlow — Briefing inteligente para escopos aprovados',
      },
    ],
  },
  twitter: {
    card: 'summary_large_image',
    title: 'ScopeFlow — Briefing inteligente, escopos aprovados',
    description: 'Transforme briefing em escopo aprovado com IA.',
    images: ['https://scopeflow.app/og-image.png'],
  },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  const schemaData = {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    name: 'ScopeFlow',
    description:
      'AI-powered briefing and scope management platform for freelancers and agencies',
    url: 'https://scopeflow.app',
    applicationCategory: 'BusinessApplication',
    operatingSystem: 'Web',
    offers: {
      '@type': 'Offer',
      price: '49',
      priceCurrency: 'BRL',
    },
    aggregateRating: {
      '@type': 'AggregateRating',
      ratingValue: '4.9',
      ratingCount: '180',
    },
  };

  return (
    <html
      lang="pt-BR"
      suppressHydrationWarning
      className={`${fraunces.variable} ${dmSans.variable}`}
    >
      <head>
        <meta charSet="utf-8" />
        <meta name="theme-color" content="#F5A623" />
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(schemaData) }}
        />
      </head>
      <body className="font-sans antialiased">
        <SessionProvider>
          <div id="root">{children}</div>
        </SessionProvider>
      </body>
    </html>
  );
}
