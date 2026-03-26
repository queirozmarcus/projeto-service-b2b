import Link from 'next/link';

interface CTASectionProps {
  headline?: string;
  subheadline?: string;
  buttonText?: string;
  buttonHref?: string;
}

export function CTASection({
  headline = "Ready to Transform Your Workflow?",
  subheadline = "Join hundreds of service providers who've eliminated scope confusion. Start free—no credit card required.",
  buttonText = "Start Your Free Trial",
  buttonHref = "/auth/register",
}: CTASectionProps) {
  return (
    <section className="bg-primary-600 px-6 py-20">
      <div className="mx-auto max-w-4xl text-center">
        {/* Headline */}
        <h2 className="mb-6 text-4xl font-bold text-white md:text-5xl">{headline}</h2>

        {/* Subheadline */}
        <p className="mb-8 text-lg text-primary-100">{subheadline}</p>

        {/* CTA Button */}
        <Link
          href={buttonHref}
          className="inline-block rounded-lg bg-white px-8 py-3 font-semibold text-primary-600 hover:bg-secondary-100 transition-colors"
        >
          {buttonText}
        </Link>
      </div>
    </section>
  );
}
