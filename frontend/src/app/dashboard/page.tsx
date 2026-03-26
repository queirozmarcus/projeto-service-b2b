'use client';

import { useEffect } from 'react';
import useSessionStore from '@/stores/useSession';
import useDashboardStore from '@/stores/useDashboardStore';
import {
  StatsGrid,
  ProposalList,
  QuickActions,
  RecentActivity,
} from '@/components/dashboard';

// Mock data for demonstration
const mockProposals = [
  {
    id: 'p1',
    clientName: 'Acme Corp',
    serviceType: 'Social Media',
    status: 'APPROVED' as const,
    createdAt: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: 'p2',
    clientName: 'XYZ Design',
    serviceType: 'Landing Page',
    status: 'DRAFT' as const,
    createdAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: 'p3',
    clientName: 'Tech Startup',
    serviceType: 'Branding',
    status: 'SENT' as const,
    createdAt: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString(),
  },
];

const mockStats = [
  {
    id: 'active',
    label: 'Propostas Ativas',
    value: 5,
    trend: '+2',
    trendDirection: 'up' as const,
  },
  {
    id: 'completed',
    label: 'Briefings Completos',
    value: 8,
    trend: '+3',
    trendDirection: 'up' as const,
  },
  {
    id: 'approval',
    label: 'Taxa de Aprovação',
    value: '87%',
    trend: '+5%',
    trendDirection: 'up' as const,
  },
  {
    id: 'clients',
    label: 'Novos Clientes',
    value: 3,
    trend: '+1',
    trendDirection: 'neutral' as const,
  },
];

const mockActivities = [
  {
    id: 'a1',
    type: 'approved' as const,
    description: 'Proposta de Acme Corp aprovada',
    timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: 'a2',
    type: 'sent' as const,
    description: 'Proposta enviada para Tech Startup',
    timestamp: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: 'a3',
    type: 'created' as const,
    description: 'Nova proposta criada para XYZ Design',
    timestamp: new Date(Date.now() - 6 * 60 * 60 * 1000).toISOString(),
  },
];

export default function DashboardPage() {
  const { user } = useSessionStore();
  const { setProposals, isLoading } = useDashboardStore();

  useEffect(() => {
    // TODO: Replace with actual API call when available
    // const fetchProposals = async () => {
    //   try {
    //     setLoading(true);
    //     const response = await api.get('/api/v1/proposals');
    //     setProposals(response.data);
    //   } catch (error) {
    //     setError('Erro ao carregar propostas');
    //   } finally {
    //     setLoading(false);
    //   }
    // };
    // fetchProposals();

    // For now, use mock data
    setProposals(mockProposals);
  }, [setProposals]);

  return (
    <div className="mx-auto max-w-7xl space-y-8 px-6 py-8">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-secondary-900">Dashboard</h1>
        <p className="mt-1 text-secondary-600">
          Bem-vindo{user ? `, ${user.fullName}` : ''}! Gerencie suas propostas
          aqui.
        </p>
      </div>

      {/* Stats Grid */}
      <StatsGrid stats={mockStats} isLoading={isLoading} />

      {/* Quick Actions */}
      <QuickActions />

      {/* Proposals Section */}
      <div>
        <h2 className="mb-4 text-2xl font-bold text-secondary-900">
          Propostas Recentes
        </h2>
        <ProposalList
          proposals={mockProposals}
          isLoading={isLoading}
        />
      </div>

      {/* Recent Activity */}
      <RecentActivity activities={mockActivities} isLoading={isLoading} />
    </div>
  );
}
