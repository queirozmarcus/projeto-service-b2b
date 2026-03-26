'use client';

interface ActivityItem {
  id: string;
  type: 'created' | 'approved' | 'sent' | 'updated';
  description: string;
  timestamp: string;
}

interface RecentActivityProps {
  activities?: ActivityItem[];
  isLoading?: boolean;
}

const typeConfig = {
  created: { dotColor: 'bg-blue-500' },
  approved: { dotColor: 'bg-emerald-500' },
  sent: { dotColor: 'bg-primary-500' },
  updated: { dotColor: 'bg-orange-500' },
};

export function RecentActivity({
  activities = [],
  isLoading = false,
}: RecentActivityProps) {
  if (isLoading) {
    return (
      <div className="rounded-2xl border border-secondary-200 bg-surface p-6 shadow-sm">
        <div className="mt-2 space-y-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="flex gap-3 animate-pulse">
              <div className="mt-1.5 h-2.5 w-2.5 flex-shrink-0 rounded-full bg-secondary-200" />
              <div className="flex-1 space-y-1.5">
                <div className="h-3.5 w-48 rounded bg-secondary-200" />
                <div className="h-3 w-24 rounded bg-secondary-100" />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (activities.length === 0) {
    return (
      <div className="rounded-2xl border border-secondary-200 bg-surface p-6 shadow-sm">
        <p className="text-sm text-secondary-500">
          Nenhuma atividade ainda. Comece criando sua primeira proposta.
        </p>
      </div>
    );
  }

  return (
    <div className="rounded-2xl border border-secondary-200 bg-surface p-6 shadow-sm">
      <div className="space-y-5">
        {activities.map((activity) => {
          const config = typeConfig[activity.type];
          const formattedTime = new Date(activity.timestamp).toLocaleTimeString('pt-BR', {
            hour: '2-digit',
            minute: '2-digit',
          });

          return (
            <div key={activity.id} className="flex gap-3">
              <div
                className={`mt-1.5 h-2.5 w-2.5 flex-shrink-0 rounded-full ${config.dotColor}`}
              />
              <div className="flex-1">
                <p className="text-sm font-medium text-ink-700">{activity.description}</p>
                <p className="mt-0.5 text-xs text-secondary-400">{formattedTime}</p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
