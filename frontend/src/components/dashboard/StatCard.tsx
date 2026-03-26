'use client';

import { ReactNode } from 'react';

interface StatCardProps {
  label: string;
  value: string | number;
  icon?: ReactNode;
  trend?: string | undefined;
  trendDirection?: 'up' | 'down' | 'neutral' | undefined;
}

export function StatCard({
  label,
  value,
  icon,
  trend,
  trendDirection = 'up',
}: StatCardProps) {
  const trendColor = {
    up: 'text-green-600',
    down: 'text-red-600',
    neutral: 'text-secondary-600',
  }[trendDirection];

  return (
    <div className="rounded-xl border border-secondary-200 bg-white p-6 shadow-sm transition hover:shadow-md">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-secondary-600">{label}</p>
          <p className="mt-2 text-3xl font-bold text-secondary-900">
            {value}
          </p>
          {trend && (
            <p className={`mt-2 text-sm font-medium ${trendColor}`}>
              {trend}
            </p>
          )}
        </div>
        {icon && <div className="text-4xl text-primary-200">{icon}</div>}
      </div>
    </div>
  );
}
