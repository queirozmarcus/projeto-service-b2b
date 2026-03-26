import Link from 'next/link';

interface FooterLink {
  label: string;
  href: string;
}

interface FooterSection {
  title: string;
  links: FooterLink[];
}

const DEFAULT_SECTIONS: FooterSection[] = [
  {
    title: 'Product',
    links: [
      { label: 'Features', href: '#features' },
      { label: 'Pricing', href: '#pricing' },
      { label: 'Security', href: '#' },
      { label: 'Roadmap', href: '#' },
    ],
  },
  {
    title: 'Company',
    links: [
      { label: 'About', href: '#' },
      { label: 'Blog', href: '#' },
      { label: 'Contact', href: '#' },
      { label: 'Press', href: '#' },
    ],
  },
  {
    title: 'Support',
    links: [
      { label: 'Help Center', href: '#' },
      { label: 'Documentation', href: '#' },
      { label: 'Status', href: '#' },
    ],
  },
  {
    title: 'Legal',
    links: [
      { label: 'Privacy Policy', href: '#' },
      { label: 'Terms of Service', href: '#' },
      { label: 'LGPD Compliance', href: '#' },
    ],
  },
];

interface FooterProps {
  sections?: FooterSection[];
  companyName?: string;
  companyDescription?: string;
}

export function Footer({
  sections = DEFAULT_SECTIONS,
  companyName = 'ScopeFlow',
  companyDescription = 'AI-powered briefing and scope management for freelancers and agencies.',
}: FooterProps) {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="border-t border-secondary-200 bg-surface-raised px-6 py-16">
      <div className="mx-auto max-w-6xl">
        <div className="mb-12 grid gap-10 md:grid-cols-5">

          {/* Brand column */}
          <div className="md:col-span-1">
            <div className="mb-4 flex items-center gap-2.5">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary-600">
                <svg className="h-4 w-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2.5}
                    d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"
                  />
                </svg>
              </div>
              <span className="text-base font-bold text-ink-900">{companyName}</span>
            </div>
            <p className="text-sm leading-relaxed text-secondary-500">{companyDescription}</p>
          </div>

          {/* Link sections */}
          {sections.map((section, index) => (
            <div key={index}>
              <h5 className="mb-4 text-sm font-semibold text-ink-700">{section.title}</h5>
              <ul className="space-y-2.5">
                {section.links.map((link, linkIndex) => (
                  <li key={linkIndex}>
                    <Link
                      href={link.href}
                      className="text-sm text-secondary-500 transition-colors hover:text-ink-800"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        {/* Bottom bar */}
        <div className="flex flex-col items-center justify-between gap-4 border-t border-secondary-200 pt-8 sm:flex-row">
          <p className="text-sm text-secondary-400">
            &copy; {currentYear} {companyName}. All rights reserved.
          </p>
          <p className="text-xs text-secondary-300">
            Built for freelancers & agencies
          </p>
        </div>
      </div>
    </footer>
  );
}
