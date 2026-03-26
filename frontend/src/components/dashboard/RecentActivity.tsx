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

export function RecentActivity({
  activities = [],
  isLoading = false,
}: RecentActivityProps) {
  const typeConfig = {
    created: { label: 'Criada', color: 'bg-blue-100', dotColor: 'bg-blue-600' },
    approved: {
      label: 'Aprovada',
      color: 'bg-green-100',
      dotColor: 'bg-green-600',
    },
    sent: { label: 'Enviada', color: 'bg-purple-100', dotColor: 'bg-purple-600' },
    updated: { label: 'Atualizada', color: 'bg-orange-100', dotColor: 'bg-orange-600' },
  };

  if (isLoading) {
    return (
      <div className="rounded-xl border border-secondary-200 bg-white p-6">
        <h2 className="text-lg font-semibold text-secondary-900">
          Atividade Recente
        </h2>
        <div className="mt-4 space-y-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="animate-pulse">
              <div className="h-4 w-64 rounded bg-secondary-200" />
              <div className="mt-2 h-3 w-32 rounded bg-secondary-100" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (activities.length === 0) {
    return (
      <div className="rounded-xl border border-secondary-200 bg-white p-6">
        <h2 className="text-lg font-semibold text-secondary-900">
          Atividade Recente
        </h2>
        <p className="mt-4 text-sm text-secondary-600">
          Nenhuma atividade ainda. Comece criando sua primeira proposta!
        </p>
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-secondary-200 bg-white p-6">
      <h2 className="text-lg font-semibold text-secondary-900">
        Atividade Recente
      </h2>
      <div className="mt-4 space-y-4">
        {activities.map((activity) => {
          const config = typeConfig[activity.type];
          const formattedTime = new Date(activity.timestamp).toLocaleTimeString(
            'pt-BR',
            { hour: '2-digit', minute: '2-digit' }
          );

          return (
            <div key={activity.id} className="flex gap-3">
              <div className={`mt-1 h-3 w-3 rounded-full flex-shrink-0 ${config.dotColor}`} />
              <div className="flex-1">
                <p className="text-sm font-medium text-secondary-900">
                  {activity.description}
                </p>
                <p className="mt-0.5 text-xs text-secondary-500">
                  {formattedTime}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
