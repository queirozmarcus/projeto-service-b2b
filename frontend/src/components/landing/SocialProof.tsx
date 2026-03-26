interface Testimonial {
  quote: string;
  author: string;
  role: string;
  company: string;
}

interface SocialProofProps {
  testimonials?: Testimonial[];
  showCompanyLogos?: boolean;
}

export function SocialProof({
  testimonials = [],
  showCompanyLogos = true,
}: SocialProofProps) {
  const defaultTestimonials: Testimonial[] = [
    {
      quote:
        'ScopeFlow cut our proposal time in half. The AI actually understands what clients mean when they ramble about their project.',
      author: 'Marina Silva',
      role: 'Founder',
      company: 'Studio de Design MarinaCo',
    },
    {
      quote:
        'Finally, a tool that speaks our language. No more back-and-forth emails about scope creep.',
      author: 'João Pedro',
      role: 'Digital Strategist',
      company: 'Agência Digital JPM',
    },
    {
      quote:
        "The approval tracking is gold. Clients can't deny they agreed to something when it's all signed and timestamped.",
      author: 'Ana Mendes',
      role: 'Project Manager',
      company: 'Landing Page House',
    },
  ];

  const displayTestimonials = testimonials.length > 0 ? testimonials : defaultTestimonials;

  return (
    <section id="social-proof" className="px-6 py-20">
      <div className="mx-auto max-w-6xl">
        {/* Testimonials */}
        <div className="mb-16">
          <h2 className="mb-12 text-center text-4xl font-bold text-secondary-900">
            Loved by Freelancers & Agencies
          </h2>

          <div className="grid gap-8 md:grid-cols-3">
            {displayTestimonials.map((testimonial, index) => (
              <div
                key={index}
                className="rounded-lg border border-secondary-200 bg-white p-8 shadow-sm"
              >
                {/* Quote */}
                <p className="mb-6 text-lg italic text-secondary-700">&quot;{testimonial.quote}&quot;</p>

                {/* Author */}
                <div>
                  <p className="font-semibold text-secondary-900">{testimonial.author}</p>
                  <p className="text-sm text-secondary-600">
                    {testimonial.role} at {testimonial.company}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Company Logos / Trust Indicators */}
        {showCompanyLogos && (
          <div className="border-t border-secondary-200 pt-12">
            <p className="mb-8 text-center text-sm font-semibold text-secondary-600 uppercase">
              Trusted by 500+ teams
            </p>
            <div className="flex flex-wrap items-center justify-center gap-8">
              {[
                '🚀 Freelancers',
                '🎨 Design Agencies',
                '📱 Social Media Experts',
                '🌐 Web Developers',
                '✍️ Content Writers',
              ].map((label, index) => (
                <div key={index} className="text-secondary-700 text-sm font-medium">
                  {label}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
