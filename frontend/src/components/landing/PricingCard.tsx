import Link from 'next/link';
import { CheckIcon } from '@heroicons/react/24/solid';

interface PricingCardProps {
  name: string;
  price: string;
  description?: string | undefined;
  features: string[];
  cta: string;
  ctaHref: string;
  highlight?: boolean;
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
  if (highlight) {
    return (
      <div className="relative flex flex-col rounded-3xl bg-primary-600 p-8 shadow-xl ring-1 ring-primary-500">
        {/* Badge */}
        <div className="absolute -top-4 left-1/2 -translate-x-1/2">
          <span className="inline-block rounded-full bg-ink-900 px-4 py-1.5 text-xs font-bold uppercase tracking-widest text-white shadow">
            Most Popular
          </span>
        </div>

        {/* Header */}
        <div className="mb-6 mt-2">
          <h3 className="text-xl font-bold text-white">{name}</h3>
          {description && <p className="mt-1 text-sm text-primary-200">{description}</p>}
        </div>

        {/* Price */}
        <div className="mb-6 flex items-end gap-1.5">
          <span className="font-display text-5xl font-black text-white">{price}</span>
          {price !== 'Contact us' && (
            <span className="mb-1 text-sm font-medium text-primary-200">/mês</span>
          )}
        </div>

        <div className="mb-6 h-px bg-primary-500" />

        {/* Features */}
        <ul className="mb-8 flex flex-col gap-3">
          {features.map((feature, i) => (
            <li key={i} className="flex items-start gap-3">
              <span className="mt-0.5 flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full bg-white/20">
                <CheckIcon className="h-3 w-3 text-white" strokeWidth={3} />
              </span>
              <span className="text-sm leading-relaxed text-primary-100">{feature}</span>
            </li>
          ))}
        </ul>

        <div className="mt-auto">
          <Link
            href={ctaHref}
            className="block w-full rounded-xl bg-white px-6 py-3.5 text-center text-sm font-bold text-primary-700 transition-all duration-200 hover:bg-primary-50 hover:shadow-sm"
          >
            {cta}
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col rounded-3xl border border-secondary-200 bg-surface p-8 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md">
      {/* Header */}
      <div className="mb-6">
        <h3 className="text-xl font-bold text-ink-800">{name}</h3>
        {description && <p className="mt-1 text-sm text-secondary-500">{description}</p>}
      </div>

      {/* Price */}
      <div className="mb-6 flex items-end gap-1.5">
        <span className="font-display text-5xl font-black text-ink-900">{price}</span>
        {price !== 'Contact us' && (
          <span className="mb-1 text-sm font-medium text-secondary-500">/mês</span>
        )}
      </div>

      <div className="mb-6 h-px bg-secondary-100" />

      {/* Features */}
      <ul className="mb-8 flex flex-col gap-3">
        {features.map((feature, i) => (
          <li key={i} className="flex items-start gap-3">
            <span className="mt-0.5 flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full bg-primary-50">
              <CheckIcon className="h-3 w-3 text-primary-600" strokeWidth={3} />
            </span>
            <span className="text-sm leading-relaxed text-secondary-700">{feature}</span>
          </li>
        ))}
      </ul>

      <div className="mt-auto">
        <Link
          href={ctaHref}
          className="block w-full rounded-xl border-2 border-primary-600 px-6 py-3.5 text-center text-sm font-bold text-primary-600 transition-all duration-200 hover:bg-primary-600 hover:text-white"
        >
          {cta}
        </Link>
      </div>
    </div>
  );
}
