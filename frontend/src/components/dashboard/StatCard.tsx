'use client';

import { ReactNode } from 'react';
import { ArrowUpIcon, ArrowDownIcon, MinusIcon } from '@heroicons/react/20/solid';

interface StatCardProps {
  label: string;
  value: string | number;
  icon?: ReactNode;
  trend?: string | undefined;
  trendDirection?: 'up' | 'down' | 'neutral' | undefined;
}

const trendConfig = {
  up: {
    text: 'text-accent-600',
    bg: 'bg-accent-50',
    border: 'border-accent-100',
    icon: <ArrowUpIcon className="h-3 w-3" />,
  },
  down: {
    text: 'text-danger-700',
    bg: 'bg-danger-50',
    border: 'border-danger-50',
    icon: <ArrowDownIcon className="h-3 w-3" />,
  },
  neutral: {
    text: 'text-secondary-500',
    bg: 'bg-secondary-50',
    border: 'border-secondary-100',
    icon: <MinusIcon className="h-3 w-3" />,
  },
};

export function StatCard({
  label,
  value,
  icon,
  trend,
  trendDirection = 'neutral',
}: StatCardProps) {
  const tc = trendConfig[trendDirection];

  return (
    <div className="group rounded-2xl border border-secondary-200 bg-surface p-6 shadow-card transition-all duration-200 hover:-translate-y-0.5 hover:shadow-card-hover">
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0 flex-1">
          <p className="text-[11px] font-bold uppercase tracking-[0.18em] text-secondary-400">
            {label}
          </p>
          <p className="mt-2 font-display text-3xl font-black text-ink-900">
            {value}
          </p>
          {trend && (
            <div
              className={`mt-3 inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-semibold ${tc.text} ${tc.bg} ${tc.border}`}
            >
              {tc.icon}
              {trend} este mês
            </div>
          )}
        </div>

        {icon && (
          <div className="flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-xl border border-primary-100 bg-primary-50 text-primary-600 transition-colors duration-200 group-hover:border-primary-200 group-hover:bg-primary-100">
            {icon}
          </div>
        )}
      </div>
    </div>
  );
}
