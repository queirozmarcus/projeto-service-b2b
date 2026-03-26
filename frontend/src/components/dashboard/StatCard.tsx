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
    text: 'text-emerald-700',
    bg: 'bg-emerald-50',
    icon: <ArrowUpIcon className="h-3 w-3" />,
  },
  down: {
    text: 'text-red-700',
    bg: 'bg-red-50',
    icon: <ArrowDownIcon className="h-3 w-3" />,
  },
  neutral: {
    text: 'text-secondary-600',
    bg: 'bg-secondary-100',
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
    <div className="group rounded-2xl border border-secondary-200 bg-surface p-6 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md">
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0 flex-1">
          <p className="text-xs font-semibold uppercase tracking-wide text-secondary-500">
            {label}
          </p>
          <p className="mt-2 font-display text-3xl font-black text-ink-900">
            {value}
          </p>
          {trend && (
            <div
              className={`mt-3 inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold ${tc.text} ${tc.bg}`}
            >
              {tc.icon}
              {trend} this month
            </div>
          )}
        </div>

        {icon && (
          <div className="flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-xl bg-primary-50 text-primary-600 transition-colors duration-200 group-hover:bg-primary-100">
            {icon}
          </div>
        )}
      </div>
    </div>
  );
}
