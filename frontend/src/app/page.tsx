import type { Metadata } from 'next';
import {
  LandingNavbar,
  Hero,
  FeatureGrid,
  PricingTable,
  SocialProof,
  CTASection,
  Footer,
} from '@/components/landing';

export const metadata: Metadata = {
  title: 'ScopeFlow - AI-Powered Briefing & Scope Approval',
  description:
    'Transform vague requirements into structured scopes with AI-powered briefing and approval workflows. Get clear agreements faster.',
  keywords: [
    'AI briefing',
    'scope management',
    'project approval',
    'B2B SaaS',
    'freelancers',
    'agencies',
  ],
  openGraph: {
    type: 'website',
    locale: 'en_US',
    url: 'https://scopeflow.app',
    siteName: 'ScopeFlow',
    title: 'ScopeFlow - AI-Powered Briefing & Scope Approval',
    description:
      'Transform vague requirements into structured scopes with AI-powered briefing and approval workflows.',
    images: [
      {
        url: 'https://scopeflow.app/og-image.png',
        width: 1200,
        height: 630,
        alt: 'ScopeFlow - Transform Briefings into Approved Scopes',
      },
    ],
  },
  twitter: {
    card: 'summary_large_image',
    title: 'ScopeFlow - AI-Powered Briefing & Scope Approval',
    description: 'Transform vague requirements into structured scopes with AI.',
    images: ['https://scopeflow.app/og-image.png'],
  },
};

const FEATURES = [
  {
    icon: '🎯',
    title: 'AI-Powered Discovery',
    description:
      'Our AI asks contextual follow-up questions that reveal hidden requirements. No more vague briefs.',
  },
  {
    icon: '📋',
    title: 'Instant Scope Generation',
    description:
      'Transform answers into structured scopes in seconds. Deliverables, timelines, assumptions—all ready.',
  },
  {
    icon: '✅',
    title: 'Traceable Approvals',
    description:
      'Clients approve with their name, email, and timestamp. Audit trail included. No disputes later.',
  },
];

const PLANS = [
  {
    name: 'Freelancer',
    price: 'R$ 29',
    description: 'Perfect for solo professionals',
    features: [
      'Up to 10 projects/month',
      'AI-powered discovery',
      'Basic scope templates',
      'Email support',
      'Approval tracking',
    ],
  },
  {
    name: 'Agency',
    price: 'R$ 99',
    description: 'For growing teams',
    features: [
      'Unlimited projects',
      'Advanced AI customization',
      'Custom templates',
      'Team collaboration (up to 10 members)',
      'Priority support',
      'Advanced analytics',
      'Custom branding',
    ],
    highlight: true,
  },
  {
    name: 'Enterprise',
    price: 'Contact us',
    description: 'For large organizations',
    features: [
      'Everything in Agency',
      'Unlimited team members',
      'Dedicated account manager',
      'Custom integrations',
      'SLA guarantee',
      'Advanced security',
      'White-label option',
    ],
  },
];

export default function HomePage() {
  return (
    <main className="min-h-screen">
      <LandingNavbar />
      <Hero />
      <FeatureGrid features={FEATURES} />
      <PricingTable plans={PLANS} />
      <SocialProof />
      <CTASection />
      <Footer />
    </main>
  );
}
