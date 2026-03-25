import type { Metadata } from 'next';
import '@/styles/globals.css';
import { SessionProvider } from '@/components/auth/SessionProvider';

export const metadata: Metadata = {
  title: 'ScopeFlow - AI-Powered Briefing & Scope Alignment',
  description:
    'Transform vague requirements into structured scopes with AI-powered briefing and approval workflows.',
  keywords:
    'AI, briefing, scope, project management, B2B, SaaS',
  authors: [{ name: 'ScopeFlow Team' }],
  viewport: 'width=device-width, initial-scale=1.0',
  robots: 'index, follow',
  openGraph: {
    type: 'website',
    locale: 'en_US',
    url: 'https://scopeflow.app',
    title: 'ScopeFlow - AI-Powered Briefing & Scope Alignment',
    description:
      'Transform vague requirements into structured scopes with AI-powered briefing.',
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <meta charSet="utf-8" />
        <meta name="theme-color" content="#0ea5e9" />
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link
          rel="preconnect"
          href="https://fonts.gstatic.com"
          crossOrigin="anonymous"
        />
      </head>
      <body className="bg-secondary-50 text-secondary-900">
        <SessionProvider>
          <div id="root">{children}</div>
        </SessionProvider>
      </body>
    </html>
  );
}
