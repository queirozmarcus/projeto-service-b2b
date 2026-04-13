'use client';

import Link from 'next/link';
import {
  DocumentPlusIcon,
  ClipboardDocumentListIcon,
  UsersIcon,
} from '@heroicons/react/24/outline';

interface QuickActionsProps {
  onNewProposal?: () => void;
  onNewBriefing?: () => void;
}

const actions = [
  {
    label: 'Nova Proposta',
    description: 'Iniciar escopo assistido por IA',
    href: '/dashboard/proposals/new',
    icon: DocumentPlusIcon,
    variant: 'primary' as const,
  },
  {
    label: 'Novo Briefing',
    description: 'Enviar link de discovery ao cliente',
    href: '#',
    icon: ClipboardDocumentListIcon,
    variant: 'secondary' as const,
  },
  {
    label: 'Ver Clientes',
    description: 'Gerenciar carteira de clientes',
    href: '/dashboard/clients',
    icon: UsersIcon,
    variant: 'ghost' as const,
  },
];

export function QuickActions({ onNewProposal, onNewBriefing }: QuickActionsProps) {
  return (
    <div className="grid gap-3 sm:grid-cols-3">
      {actions.map((action) => {
        const Icon = action.icon;
        const isNew = action.label === 'Nova Proposta';
        const isBriefing = action.label === 'Novo Briefing';

        const base =
          'group flex items-center gap-3 rounded-2xl border px-5 py-4 transition-all duration-200 hover:-translate-y-0.5';

        const style =
          action.variant === 'primary'
            ? `${base} border-primary-400 bg-primary-500 text-ink-900 shadow-sm hover:bg-primary-400 hover:shadow`
            : action.variant === 'secondary'
            ? `${base} border-primary-200 bg-primary-50 text-primary-700 hover:bg-primary-100`
            : `${base} border-secondary-200 bg-surface text-secondary-700 hover:border-secondary-300 hover:bg-secondary-50`;

        if (isNew && onNewProposal) {
          return (
            <button
              key={action.label}
              onClick={onNewProposal}
              className={style}
            >
              <Icon className="h-5 w-5 flex-shrink-0" />
              <div className="text-left">
                <p className="text-sm font-semibold">{action.label}</p>
                <p
                  className={`text-xs ${
                    action.variant === 'primary' ? 'text-ink-700' : 'text-secondary-500'
                  }`}
                >
                  {action.description}
                </p>
              </div>
            </button>
          );
        }

        if (isBriefing && onNewBriefing) {
          return (
            <button
              key={action.label}
              onClick={onNewBriefing}
              className={style}
            >
              <Icon className="h-5 w-5 flex-shrink-0" />
              <div className="text-left">
                <p className="text-sm font-semibold">{action.label}</p>
                <p className="text-xs text-primary-500">{action.description}</p>
              </div>
            </button>
          );
        }

        return (
          <Link key={action.label} href={action.href} className={style}>
            <Icon className="h-5 w-5 flex-shrink-0" />
            <div className="text-left">
              <p className="text-sm font-semibold">{action.label}</p>
              <p
                className={`text-xs ${
                  action.variant === 'primary' ? 'text-ink-700' : 'text-secondary-500'
                }`}
              >
                {action.description}
              </p>
            </div>
          </Link>
        );
      })}
    </div>
  );
}
