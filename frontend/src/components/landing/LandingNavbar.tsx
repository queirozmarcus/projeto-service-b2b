import Link from 'next/link';

export function LandingNavbar() {
  return (
    <nav className="sticky top-0 z-50 border-b border-secondary-200 bg-white">
      <div className="mx-auto max-w-7xl px-6 py-4">
        <div className="flex items-center justify-between">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2">
            <span className="text-2xl font-bold text-primary-600">ScopeFlow</span>
          </Link>

          {/* Feature Links */}
          <div className="hidden gap-8 md:flex">
            <Link
              href="#features"
              className="text-secondary-700 hover:text-primary-600 transition-colors"
            >
              Features
            </Link>
            <Link
              href="#pricing"
              className="text-secondary-700 hover:text-primary-600 transition-colors"
            >
              Pricing
            </Link>
            <Link
              href="#social-proof"
              className="text-secondary-700 hover:text-primary-600 transition-colors"
            >
              Why ScopeFlow
            </Link>
          </div>

          {/* Auth Links */}
          <div className="flex gap-4">
            <Link
              href="/auth/login"
              className="rounded-lg px-4 py-2 text-secondary-700 hover:bg-secondary-100 transition-colors"
            >
              Log in
            </Link>
            <Link
              href="/auth/register"
              className="rounded-lg bg-primary-600 px-4 py-2 text-white font-medium hover:bg-primary-700 transition-colors"
            >
              Get Started
            </Link>
          </div>
        </div>
      </div>
    </nav>
  );
}
