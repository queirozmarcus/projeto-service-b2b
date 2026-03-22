import Link from 'next/link';

export default function HomePage() {
  return (
    <main className="min-h-screen">
      {/* Navigation */}
      <nav className="border-b border-secondary-200 bg-white px-6 py-4">
        <div className="mx-auto max-w-7xl">
          <div className="flex items-center justify-between">
            <h1 className="text-2xl font-bold text-primary-600">ScopeFlow</h1>
            <div className="flex gap-4">
              <Link
                href="/auth/login"
                className="rounded-lg px-4 py-2 text-secondary-700 hover:bg-secondary-100"
              >
                Login
              </Link>
              <Link
                href="/auth/register"
                className="rounded-lg bg-primary-600 px-4 py-2 text-white hover:bg-primary-700"
              >
                Get Started
              </Link>
            </div>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="bg-gradient-to-b from-primary-50 to-white px-6 py-20">
        <div className="mx-auto max-w-4xl text-center">
          <h2 className="mb-6 text-5xl font-bold text-secondary-900">
            Transform Vague Requirements into Structured Scopes
          </h2>
          <p className="mb-8 text-xl text-secondary-600">
            AI-powered briefing sessions that extract clarity from confusion.
            Get scope agreements in hours, not weeks.
          </p>
          <div className="flex justify-center gap-4">
            <Link
              href="/auth/register"
              className="rounded-lg bg-primary-600 px-8 py-3 font-semibold text-white hover:bg-primary-700"
            >
              Start Free Trial
            </Link>
            <Link
              href="#features"
              className="rounded-lg border-2 border-primary-600 px-8 py-3 font-semibold text-primary-600 hover:bg-primary-50"
            >
              Learn More
            </Link>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="px-6 py-20">
        <div className="mx-auto max-w-6xl">
          <h3 className="mb-12 text-center text-3xl font-bold text-secondary-900">
            Why ScopeFlow?
          </h3>
          <div className="grid gap-8 md:grid-cols-3">
            {/* Feature 1 */}
            <div className="rounded-lg border border-secondary-200 bg-white p-8">
              <div className="mb-4 text-4xl">🤖</div>
              <h4 className="mb-3 text-xl font-semibold text-secondary-900">
                AI-Powered Briefing
              </h4>
              <p className="text-secondary-600">
                Ask the right follow-up questions automatically. Our AI learns
                what matters for your service type.
              </p>
            </div>

            {/* Feature 2 */}
            <div className="rounded-lg border border-secondary-200 bg-white p-8">
              <div className="mb-4 text-4xl">📋</div>
              <h4 className="mb-3 text-xl font-semibold text-secondary-900">
                Instant Scope Generation
              </h4>
              <p className="text-secondary-600">
                From answers to scope in seconds. Deliverables, timelines, and
                estimates. All structured and ready.
              </p>
            </div>

            {/* Feature 3 */}
            <div className="rounded-lg border border-secondary-200 bg-white p-8">
              <div className="mb-4 text-4xl">✅</div>
              <h4 className="mb-3 text-xl font-semibold text-secondary-900">
                Approval Workflows
              </h4>
              <p className="text-secondary-600">
                Traceable sign-offs. Multi-party approvals. Audit trail. Know
                exactly who agreed to what.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="bg-primary-600 px-6 py-16">
        <div className="mx-auto max-w-4xl text-center">
          <h3 className="mb-4 text-3xl font-bold text-white">
            Ready to structure your chaos?
          </h3>
          <p className="mb-8 text-lg text-primary-100">
            Join agências, freelancers, and studios already using ScopeFlow.
          </p>
          <Link
            href="/auth/register"
            className="inline-block rounded-lg bg-white px-8 py-3 font-semibold text-primary-600 hover:bg-secondary-100"
          >
            Start Your Free Trial
          </Link>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-secondary-200 bg-white px-6 py-12">
        <div className="mx-auto max-w-6xl">
          <div className="grid gap-8 md:grid-cols-4">
            <div>
              <h5 className="mb-4 font-bold text-secondary-900">ScopeFlow</h5>
              <p className="text-sm text-secondary-600">
                AI-powered briefing for service providers.
              </p>
            </div>
            <div>
              <h5 className="mb-4 font-semibold text-secondary-900">Product</h5>
              <ul className="space-y-2 text-sm text-secondary-600">
                <li>
                  <Link href="#" className="hover:text-primary-600">
                    Features
                  </Link>
                </li>
                <li>
                  <Link href="#" className="hover:text-primary-600">
                    Pricing
                  </Link>
                </li>
                <li>
                  <Link href="#" className="hover:text-primary-600">
                    Security
                  </Link>
                </li>
              </ul>
            </div>
            <div>
              <h5 className="mb-4 font-semibold text-secondary-900">Company</h5>
              <ul className="space-y-2 text-sm text-secondary-600">
                <li>
                  <Link href="#" className="hover:text-primary-600">
                    About
                  </Link>
                </li>
                <li>
                  <Link href="#" className="hover:text-primary-600">
                    Blog
                  </Link>
                </li>
                <li>
                  <Link href="#" className="hover:text-primary-600">
                    Contact
                  </Link>
                </li>
              </ul>
            </div>
            <div>
              <h5 className="mb-4 font-semibold text-secondary-900">Legal</h5>
              <ul className="space-y-2 text-sm text-secondary-600">
                <li>
                  <Link href="#" className="hover:text-primary-600">
                    Privacy
                  </Link>
                </li>
                <li>
                  <Link href="#" className="hover:text-primary-600">
                    Terms
                  </Link>
                </li>
                <li>
                  <Link href="#" className="hover:text-primary-600">
                    Status
                  </Link>
                </li>
              </ul>
            </div>
          </div>
          <div className="mt-8 border-t border-secondary-200 pt-8 text-center text-sm text-secondary-600">
            <p>&copy; 2026 ScopeFlow. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </main>
  );
}
