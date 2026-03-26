'use client';

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
            className="rounded-xl border border-secondary-200 bg-white p-6 animate-pulse"
          >
            <div className="h-4 w-32 rounded bg-secondary-200" />
            <div className="mt-4 h-8 w-16 rounded bg-secondary-200" />
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
          trend={stat.trend ?? undefined}
          trendDirection={stat.trendDirection ?? undefined}
        />
      ))}
    </div>
  );
}
