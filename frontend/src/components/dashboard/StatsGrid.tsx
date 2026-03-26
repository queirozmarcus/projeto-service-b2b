'use client';

import {
  DocumentTextIcon,
  ClipboardDocumentCheckIcon,
  ChartBarIcon,
  UsersIcon,
} from '@heroicons/react/24/outline';
import { StatCard } from './StatCard';

interface StatsGridProps {
  stats?: Array<{
    id: string;
    label: string;
    value: string | number;
    trend?: string;
    trendDirection?: 'up' | 'down' | 'neutral';
  }>;
  isLoading?: boolean;
}

const statIcons: Record<string, React.ReactNode> = {
  active: <DocumentTextIcon className="h-6 w-6" />,
  completed: <ClipboardDocumentCheckIcon className="h-6 w-6" />,
  approval: <ChartBarIcon className="h-6 w-6" />,
  clients: <UsersIcon className="h-6 w-6" />,
};

const defaultStats = [
  {
    id: 'active',
    label: 'Propostas Ativas',
    value: 0,
    trend: '+0',
    trendDirection: 'neutral' as const,
  },
  {
    id: 'completed',
    label: 'Briefings Completos',
    value: 0,
    trend: '+0',
    trendDirection: 'neutral' as const,
  },
  {
    id: 'approval',
    label: 'Taxa de Aprovação',
    value: '0%',
    trend: '+0%',
    trendDirection: 'neutral' as const,
  },
  {
    id: 'clients',
    label: 'Novos Clientes',
    value: 0,
    trend: '+0',
    trendDirection: 'neutral' as const,
  },
];

export function StatsGrid({ stats = defaultStats, isLoading = false }: StatsGridProps) {
  if (isLoading) {
    return (
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        {defaultStats.map((stat) => (
          <div
            key={stat.id}
            className="rounded-2xl border border-secondary-200 bg-surface p-6 animate-pulse"
          >
            <div className="flex items-start justify-between">
              <div>
                <div className="h-4 w-28 rounded-lg bg-secondary-200" />
                <div className="mt-4 h-8 w-20 rounded-lg bg-secondary-200" />
              </div>
              <div className="h-12 w-12 rounded-xl bg-secondary-100" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
      {stats.map((stat) => (
        <StatCard
          key={stat.id}
          label={stat.label}
          value={stat.value}
          icon={statIcons[stat.id]}
          trend={stat.trend ?? undefined}
          trendDirection={stat.trendDirection ?? undefined}
        />
      ))}
    </div>
  );
}
