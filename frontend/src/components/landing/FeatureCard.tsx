interface FeatureCardProps {
  icon: string;
  title: string;
  description: string;
}

export function FeatureCard({ icon, title, description }: FeatureCardProps) {
  return (
    <div className="flex flex-col gap-4 rounded-lg border border-secondary-200 bg-white p-8 shadow-sm hover:shadow-md transition-shadow">
      {/* Icon */}
      <div className="text-5xl">{icon}</div>

      {/* Title */}
      <h3 className="text-xl font-semibold text-secondary-900">{title}</h3>

      {/* Description */}
      <p className="text-secondary-600">{description}</p>
    </div>
  );
}
