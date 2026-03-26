import { PricingCard } from './PricingCard';

interface Plan {
  name: string;
  price: string;
  description?: string;
  features: string[];
  highlight?: boolean;
}

interface PricingTableProps {
  plans: Plan[];
  title?: string;
  subtitle?: string;
}

export function PricingTable({
  plans,
  title = 'Simple, Transparent Pricing',
  subtitle = 'Choose the plan that fits your team',
}: PricingTableProps) {
  return (
    <section id="pricing" className="bg-secondary-50 px-6 py-20">
      <div className="mx-auto max-w-6xl">
        {/* Header */}
        <div className="mb-12 text-center">
          <h2 className="mb-4 text-4xl font-bold text-secondary-900">{title}</h2>
          {subtitle && <p className="text-lg text-secondary-600">{subtitle}</p>}
        </div>

        {/* Pricing Cards */}
        <div className="grid gap-8 md:grid-cols-3">
          {plans.map((plan, index) => (
            <PricingCard
              key={index}
              name={plan.name}
              price={plan.price}
              description={plan.description ?? undefined}
              features={plan.features}
              cta="Get Started"
              ctaHref="/auth/register"
              highlight={plan.highlight ?? false}
            />
          ))}
        </div>
      </div>
    </section>
  );
}
