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
      { label: 'Contact Support', href: '#' },
    ],
  },
  {
    title: 'Legal',
    links: [
      { label: 'Privacy Policy', href: '#' },
      { label: 'Terms of Service', href: '#' },
      { label: 'Cookie Policy', href: '#' },
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
  companyDescription = 'AI-powered briefing and scope management for service providers.',
}: FooterProps) {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="border-t border-secondary-200 bg-white px-6 py-12">
      <div className="mx-auto max-w-6xl">
        {/* Footer Grid */}
        <div className="mb-8 grid gap-8 md:grid-cols-5">
          {/* Company Info */}
          <div className="md:col-span-1">
            <h5 className="mb-4 font-bold text-secondary-900">{companyName}</h5>
            <p className="text-sm text-secondary-600">{companyDescription}</p>
          </div>

          {/* Footer Sections */}
          {sections.map((section, index) => (
            <div key={index}>
              <h5 className="mb-4 font-semibold text-secondary-900">{section.title}</h5>
              <ul className="space-y-2">
                {section.links.map((link, linkIndex) => (
                  <li key={linkIndex}>
                    <Link
                      href={link.href}
                      className="text-sm text-secondary-600 hover:text-primary-600 transition-colors"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        {/* Bottom Section */}
        <div className="border-t border-secondary-200 pt-8 text-center">
          <p className="text-sm text-secondary-600">
            &copy; {currentYear} {companyName}. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  );
}
