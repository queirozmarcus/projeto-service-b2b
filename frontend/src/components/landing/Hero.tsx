import Link from 'next/link';

export function Hero() {
  return (
    <section className="bg-gradient-to-b from-primary-50 to-white px-6 py-20">
      <div className="mx-auto max-w-4xl text-center">
        {/* Main Headline */}
        <h1 className="mb-6 text-5xl font-bold text-secondary-900 md:text-6xl">
          Transform Briefings into Approved Scopes with AI
        </h1>

        {/* Subheadline */}
        <p className="mb-8 text-xl text-secondary-600 md:text-2xl">
          Let IA guide your clients through structured discovery. Get clear agreements,
          faster approvals, and eliminate rework—all in one platform.
        </p>

        {/* CTA Buttons */}
        <div className="flex flex-col gap-4 sm:flex-row sm:justify-center">
          <Link
            href="/auth/register"
            className="rounded-lg bg-primary-600 px-8 py-3 font-semibold text-white hover:bg-primary-700 transition-colors"
          >
            Start Free Trial
          </Link>
          <Link
            href="#features"
            className="rounded-lg border-2 border-primary-600 px-8 py-3 font-semibold text-primary-600 hover:bg-primary-50 transition-colors"
          >
            See Features
          </Link>
        </div>

        {/* Social Proof */}
        <div className="mt-12 text-sm text-secondary-600">
          <p>✨ Used by 500+ freelancers and agencies worldwide</p>
        </div>
      </div>
    </section>
  );
}
