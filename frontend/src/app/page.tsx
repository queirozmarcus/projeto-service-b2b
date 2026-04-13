import type { Metadata } from 'next';
import {
  LandingNavbar,
  Hero,
  ProblemSolution,
  FeatureGrid,
  HowItWorks,
  PricingTable,
  SocialProof,
  FAQ,
  CTASection,
  Footer,
} from '@/components/landing';

export const metadata: Metadata = {
  title: 'ScopeFlow — Briefing inteligente, escopos aprovados',
  description:
    'Transforme conversas vagas em escopos claros, aprovados e rastreáveis com uma experiência de briefing orientada por IA.',
  keywords: [
    'briefing com IA',
    'gestao de escopo',
    'aprovacao de projeto',
    'saas b2b',
    'freelancers',
    'agencias',
    'consultoria digital',
  ],
  openGraph: {
    type: 'website',
    locale: 'pt_BR',
    url: 'https://scopeflow.app',
    siteName: 'ScopeFlow',
    title: 'ScopeFlow — Briefing inteligente, escopos aprovados',
    description:
      'Transforme conversas vagas em escopos claros, aprovados e rastreáveis com IA.',
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

const FEATURES = [
  {
    icon: '01',
    title: 'Descoberta guiada',
    description:
      'A IA conduz perguntas inteligentes para revelar objetivo, escopo, restrições e prioridades logo no primeiro contato.',
  },
  {
    icon: '02',
    title: 'Escopo pronto para validar',
    description:
      'Respostas viram um documento claro com entregáveis, premissas, cronograma e critérios de aprovação.',
  },
  {
    icon: '03',
    title: 'Aprovação sem ruído',
    description:
      'Cliente aprova com contexto e rastreabilidade. Menos retrabalho, menos discussão e mais previsibilidade comercial.',
  },
];

const PLANS = [
  {
    name: 'Starter',
    price: 'R$ 49',
    description: 'Para autônomos e pequenos estúdios',
    features: [
      'Até 15 briefings por mês',
      'Fluxo guiado com IA',
      'Escopo com entregáveis e premissas',
      'Histórico de aprovações',
      'Suporte por email',
    ],
  },
  {
    name: 'Growth',
    price: 'R$ 149',
    description: 'Para operações comerciais e equipes de entrega',
    features: [
      'Briefings ilimitados',
      'Templates por serviço',
      'Colaboração para equipe',
      'Branding da sua operação',
      'Prioridade no suporte',
      'Relatórios de conversão',
      'Aprovação compartilhável',
    ],
    highlight: true,
  },
  {
    name: 'Enterprise',
    price: 'Sob consulta',
    description: 'Para times com processo, compliance e integrações',
    features: [
      'Tudo do plano Growth',
      'SSO e governança',
      'Integrações customizadas',
      'Ambiente white-label',
      'SLA e onboarding dedicado',
      'Políticas avançadas de segurança',
    ],
  },
];

export default function HomePage() {
  return (
    <main className="bg-void">
      <LandingNavbar />
      <Hero />
      <ProblemSolution />
      <FeatureGrid features={FEATURES} />
      <HowItWorks />
      <PricingTable plans={PLANS} />
      <SocialProof />
      <FAQ />
      <CTASection />
      <Footer />
    </main>
  );
}
