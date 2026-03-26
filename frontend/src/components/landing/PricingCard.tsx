import Link from 'next/link';

interface PricingCardProps {
  name: string;
  price: string;
  description?: string | undefined;
  features: string[];
  cta: string;
  ctaHref: string;
  highlight?: boolean | undefined;
}

export function PricingCard({
  name,
  price,
  description,
  features,
  cta,
  ctaHref,
  highlight = false,
}: PricingCardProps) {
  return (
    <div
      className={`relative flex flex-col gap-6 rounded-lg border p-8 transition-shadow ${
        highlight
          ? 'border-primary-600 bg-primary-50 shadow-lg ring-2 ring-primary-600/10'
          : 'border-secondary-200 bg-white shadow-sm hover:shadow-md'
      }`}
    >
      {/* Highlight Badge */}
      {highlight && (
        <div className="absolute -top-4 left-1/2 -translate-x-1/2">
          <span className="inline-block rounded-full bg-primary-600 px-4 py-1 text-sm font-semibold text-white">
            Most Popular
          </span>
        </div>
      )}

      {/* Plan Name */}
      <div>
        <h3 className="text-2xl font-bold text-secondary-900">{name}</h3>
        {description && <p className="mt-2 text-secondary-600">{description}</p>}
      </div>

      {/* Price */}
      <div className={`text-4xl font-bold ${highlight ? 'text-primary-700' : 'text-secondary-900'}`}>
        {price}
        {price !== 'Contact us' && <span className="text-sm font-normal text-secondary-600">/month</span>}
      </div>

      {/* Features List */}
      <ul className="flex flex-col gap-3">
        {features.map((feature, index) => (
          <li key={index} className="flex gap-3 text-secondary-700">
            <span className="text-lg text-success-500 flex-shrink-0">✓</span>
            <span>{feature}</span>
          </li>
        ))}
      </ul>

      {/* CTA Button */}
      <Link
        href={ctaHref}
        className={`rounded-lg px-6 py-3 font-semibold text-center transition-colors ${
          highlight
            ? 'bg-primary-600 text-white hover:bg-primary-700'
            : 'border-2 border-primary-600 text-primary-600 hover:bg-primary-50'
        }`}
      >
        {cta}
      </Link>
    </div>
  );
}
