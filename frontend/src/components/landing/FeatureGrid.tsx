import { FeatureCard } from './FeatureCard';

interface Feature {
  icon: string;
  title: string;
  description: string;
}

interface FeatureGridProps {
  features: Feature[];
  title?: string;
  subtitle?: string;
}

export function FeatureGrid({
  features,
  title = 'Why ScopeFlow?',
  subtitle,
}: FeatureGridProps) {
  return (
    <section id="features" className="px-6 py-20">
      <div className="mx-auto max-w-6xl">
        {/* Header */}
        <div className="mb-12 text-center">
          <h2 className="mb-4 text-4xl font-bold text-secondary-900">{title}</h2>
          {subtitle && <p className="text-lg text-secondary-600">{subtitle}</p>}
        </div>

        {/* Features Grid */}
        <div className="grid gap-8 md:grid-cols-3">
          {features.map((feature, index) => (
            <FeatureCard key={index} {...feature} />
          ))}
        </div>
      </div>
    </section>
  );
}
