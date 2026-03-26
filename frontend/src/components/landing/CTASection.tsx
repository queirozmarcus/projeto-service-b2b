import Link from 'next/link';
import { ArrowRightIcon } from '@heroicons/react/24/outline';

interface CTASectionProps {
  headline?: string;
  subheadline?: string;
  buttonText?: string;
  buttonHref?: string;
}

export function CTASection({
  headline = 'Ready to Transform Your Workflow?',
  subheadline = 'Join hundreds of service providers who have eliminated scope confusion. Start free — no credit card required.',
  buttonText = 'Start Your Free Trial',
  buttonHref = '/auth/register',
}: CTASectionProps) {
  return (
    <section className="relative overflow-hidden bg-ink-900 px-6 py-24 lg:py-32">
      {/*
        Subtle decorative circles — large, very low opacity.
        They add depth without distracting from the text.
      */}
      <div
        aria-hidden
        className="pointer-events-none absolute -right-40 -top-40 h-[500px] w-[500px] rounded-full border border-white/5"
      />
      <div
        aria-hidden
        className="pointer-events-none absolute -bottom-32 left-1/3 h-80 w-80 rounded-full bg-primary-600/10"
      />
      <div
        aria-hidden
        className="pointer-events-none absolute left-0 top-0 h-full w-full"
        style={{
          background:
            'radial-gradient(ellipse at 30% 50%, rgba(14,165,233,0.07) 0%, transparent 60%)',
        }}
      />

      <div className="relative mx-auto max-w-3xl text-center">
        <p className="section-label mb-6 text-primary-400">
          Get Started Today
        </p>

        <h2 className="font-display text-4xl font-black leading-tight text-white md:text-5xl lg:text-[56px]">
          {headline}
        </h2>

        <p className="mx-auto mt-6 max-w-xl text-lg leading-relaxed text-secondary-400">
          {subheadline}
        </p>

        <div className="mt-10 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
          <Link
            href={buttonHref}
            className="group inline-flex items-center gap-2.5 rounded-xl bg-primary-500 px-8 py-4 text-base font-bold text-white shadow-lg transition-all duration-200 hover:-translate-y-0.5 hover:bg-primary-400 hover:shadow-xl active:translate-y-0"
          >
            {buttonText}
            <ArrowRightIcon className="h-4 w-4 transition-transform duration-200 group-hover:translate-x-1" />
          </Link>
          <span className="text-sm text-secondary-500">Free 14-day trial</span>
        </div>
      </div>
    </section>
  );
}
