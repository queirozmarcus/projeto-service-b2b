# Landing Page Component Architecture

## Overview

Landing page components are **composable, server-rendered, and stateless**. They follow Next.js 15 App Router conventions and are fully compatible with Server-Side Generation (SSG).

## Component Hierarchy

```
page.tsx (Server Component)
├── LandingNavbar
├── Hero
├── FeatureGrid
│   └── FeatureCard[] (array)
├── PricingTable
│   └── PricingCard[] (array)
├── SocialProof
├── CTASection
└── Footer
```

## Component Reference

### LandingNavbar
**Purpose:** Sticky navigation bar with brand, feature links, and auth CTAs.

**Props:** None (hardcoded data)

**Key Features:**
- Sticky positioning (z-50)
- Responsive: hidden menu on mobile, visible on md+
- Anchor links for in-page navigation (#features, #pricing, #social-proof)
- Auth links to /auth/login and /auth/register

**Usage:**
```tsx
<LandingNavbar />
```

### Hero
**Purpose:** Hero section with headline, subheadline, and CTAs.

**Props:** None (hardcoded data)

**Key Features:**
- Gradient background (primary-50 to white)
- Responsive text sizing (text-5xl → text-6xl on md+)
- Two CTA buttons (primary and outline)
- Social proof text

**Usage:**
```tsx
<Hero />
```

### FeatureGrid
**Purpose:** Renders a grid of feature cards.

**Props:**
```typescript
interface FeatureGridProps {
  features: Feature[];
  title?: string;        // Default: "Why ScopeFlow?"
  subtitle?: string;
}

interface Feature {
  icon: string;          // Emoji or icon string
  title: string;
  description: string;
}
```

**Key Features:**
- 3-column grid on md+, single column on mobile
- Configurable title and subtitle
- Maps features array to FeatureCard components

**Usage:**
```tsx
<FeatureGrid
  features={[
    { icon: '🎯', title: 'Discovery', description: 'AI asks questions' },
    { icon: '📋', title: 'Scope', description: 'Instant generation' },
    { icon: '✅', title: 'Approval', description: 'Traceable sign-offs' },
  ]}
/>
```

### FeatureCard
**Purpose:** Individual feature card component.

**Props:**
```typescript
interface FeatureCardProps {
  icon: string;
  title: string;
  description: string;
}
```

**Key Features:**
- Border + shadow + hover effects
- Large icon display
- Semantic heading (h3)

**Usage:**
```tsx
<FeatureCard
  icon="🎯"
  title="AI-Powered Discovery"
  description="Our AI asks contextual follow-up questions..."
/>
```

### PricingTable
**Purpose:** Renders a grid of pricing plans.

**Props:**
```typescript
interface PricingTableProps {
  plans: Plan[];
  title?: string;        // Default: "Simple, Transparent Pricing"
  subtitle?: string;     // Default: "Choose the plan that fits your team"
}

interface Plan {
  name: string;
  price: string;
  description?: string;
  features: string[];
  highlight?: boolean;   // Highlights "Most Popular" plan
}
```

**Key Features:**
- 3-column grid on md+
- Highlight badge for "Most Popular" tier
- Dynamic button styling based on highlight state
- Checkmark icons for features

**Usage:**
```tsx
<PricingTable
  plans={[
    {
      name: 'Freelancer',
      price: 'R$ 29',
      features: ['10 projects/month', 'AI discovery', 'Basic templates'],
    },
    {
      name: 'Agency',
      price: 'R$ 99',
      features: ['Unlimited projects', 'Advanced AI', 'Custom templates'],
      highlight: true,
    },
    {
      name: 'Enterprise',
      price: 'Contact us',
      features: ['Everything', 'Dedicated support', 'Custom integration'],
    },
  ]}
/>
```

### PricingCard
**Purpose:** Individual pricing tier card.

**Props:**
```typescript
interface PricingCardProps {
  name: string;
  price: string;
  description?: string;
  features: string[];
  cta: string;           // Button text
  ctaHref: string;       // Button destination
  highlight?: boolean;   // Shows "Most Popular" badge
}
```

**Key Features:**
- Conditional styling for highlight state
- "Most Popular" badge positioned absolutely
- Feature list with checkmarks (success-500)
- Configurable CTA button

**Usage:**
```tsx
<PricingCard
  name="Agency"
  price="R$ 99"
  features={['Unlimited projects', 'Advanced AI']}
  cta="Get Started"
  ctaHref="/auth/register"
  highlight={true}
/>
```

### SocialProof
**Purpose:** Testimonials and social proof section.

**Props:**
```typescript
interface SocialProofProps {
  testimonials?: Testimonial[];  // Optional override
  showCompanyLogos?: boolean;    // Default: true
}

interface Testimonial {
  quote: string;
  author: string;
  role: string;
  company: string;
}
```

**Key Features:**
- Default 3 testimonials (hardcoded in component)
- Trust indicator badges (🚀 Freelancers, 🎨 Agencies, etc.)
- Configurable to show custom testimonials
- Italic quote styling

**Usage:**
```tsx
<SocialProof />

// Or with custom testimonials:
<SocialProof
  testimonials={[
    {
      quote: 'Best tool ever!',
      author: 'John Doe',
      role: 'CEO',
      company: 'Acme',
    },
  ]}
/>
```

### CTASection
**Purpose:** Full-width call-to-action section.

**Props:**
```typescript
interface CTASectionProps {
  headline?: string;
  subheadline?: string;
  buttonText?: string;
  buttonHref?: string;
}
```

**Key Features:**
- Primary color background (primary-600)
- Configurable content and button
- Centered layout
- White CTA button

**Usage:**
```tsx
<CTASection
  headline="Ready to Transform Your Workflow?"
  subheadline="Join hundreds of service providers..."
  buttonText="Start Your Free Trial"
  buttonHref="/auth/register"
/>
```

### Footer
**Purpose:** Multi-column footer with links and company info.

**Props:**
```typescript
interface FooterProps {
  sections?: FooterSection[];
  companyName?: string;
  companyDescription?: string;
}

interface FooterSection {
  title: string;
  links: FooterLink[];
}

interface FooterLink {
  label: string;
  href: string;
}
```

**Key Features:**
- 5-column layout (1 company info + 4 link sections)
- Default sections: Product, Company, Support, Legal
- Dynamic copyright year
- Hover states on links

**Usage:**
```tsx
<Footer
  companyName="ScopeFlow"
  companyDescription="AI-powered briefing for service providers."
  sections={[
    {
      title: 'Product',
      links: [
        { label: 'Features', href: '#features' },
        { label: 'Pricing', href: '#pricing' },
      ],
    },
    // ... more sections
  ]}
/>
```

## Design System Integration

### Colors
All components use design tokens from `tailwind.config.js`:
- **Primary (Sky Blue):** primary-50 through primary-900
- **Secondary (Slate):** secondary-50 through secondary-900
- **Semantic:** success-500, warning-500, danger-500

### Typography
- **Headings:** h1-h6 with semantic HTML (no font size overrides)
- **Body text:** Default prose styles
- **Links:** primary-600 with hover state (primary-700)

### Spacing
Tailwind default spacing (2, 4, 6, 8, 12, etc.) plus custom:
- `spacing.18`: 4.5rem
- `spacing.112`: 28rem
- `spacing.128`: 32rem

### Border Radius
- `rounded-lg`: 0.5rem (primary button/card radius)
- `rounded-full`: 9999px (badge radius)

## Responsive Design

All components follow mobile-first design:
- **Mobile:** Single column, full width
- **md+ (768px+):** Grid layouts activate, spacing increases

Example:
```tsx
<div className="grid gap-8 md:grid-cols-3">
  {/* Single column on mobile, 3 columns on md+ */}
</div>
```

## Performance Considerations

1. **Server-Side Rendering:** All components are Server Components (no 'use client')
2. **Zero JavaScript:** Landing page generates as static HTML during build
3. **Image Optimization:** Uses Next.js Image component if needed (currently emoji/text only)
4. **Link Prefetching:** Next.js Link automatically prefetches routes
5. **No External Scripts:** Inline JSON-LD only, no third-party tags (GA added post-launch)

## Accessibility

1. **Semantic HTML:** Proper heading hierarchy (h1 → h6)
2. **ARIA Labels:** Links have descriptive text
3. **Color Contrast:** All text meets WCAG AA standards
4. **Focus States:** All interactive elements have focus styles
5. **Motion:** Respects `prefers-reduced-motion` (defined in globals.css)

## SEO Best Practices

1. **Meta Tags:** Set in page.tsx via generateMetadata()
2. **Structured Data:** JSON-LD (Schema.org SoftwareApplication) in layout.tsx
3. **Sitemap:** app/sitemap.ts with proper changeFrequency/priority
4. **Robots.txt:** public/robots.txt allows indexing of landing pages
5. **Open Graph:** Full og: and twitter: meta tags
6. **Canonical URLs:** Set in page metadata

## Data Flow

```
page.tsx (server)
│
├── FEATURES (hardcoded array)
│   └── FeatureGrid
│       └── FeatureCard[] (map)
│
├── PLANS (hardcoded array)
│   └── PricingTable
│       └── PricingCard[] (map)
│
└── Other components (no data)
    └── Props with defaults
```

All data is hardcoded in page.tsx. To fetch from API later:
1. Mark page.tsx with `revalidate = 3600` (ISR)
2. Use `fetch()` to get FEATURES, PLANS from API
3. Components remain unchanged

## Testing

Since components are pure presentational, test via:
1. **Visual regression:** Screenshots of each section
2. **Component storybook:** Optional integration for shared UI
3. **E2E:** Test landing page flow (scroll to features, click CTA, etc.)
4. **Lighthouse:** Verify performance, SEO, accessibility scores

Example E2E test (Playwright):
```typescript
test('landing page hero has correct headline', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: /briefings into approved scopes/i })).toBeVisible();
});
```

## Modification Guide

### Adding a New Feature Card
1. Update `FEATURES` array in page.tsx
2. No component changes needed (FeatureGrid maps array)

### Adding a Pricing Tier
1. Update `PLANS` array in page.tsx
2. No component changes needed

### Changing Navbar Links
1. Edit LandingNavbar.tsx (hardcoded)
2. Update href values

### Changing Footer Links
1. Pass custom `sections` prop to Footer
2. Or hardcode in page.tsx

### Changing Colors
1. Update tailwind.config.js color tokens
2. Or use specific class names (e.g., `bg-primary-600`)

## Future Enhancements

- **Internationalisation:** i18n wrapper for all hardcoded strings
- **Dark Mode:** `dark:` Tailwind utilities
- **Animations:** Framer Motion for scroll animations
- **Blog Integration:** Latest posts section
- **Analytics:** Segment/Mixpanel tracking
- **Form Integration:** Newsletter signup form
- **Video Modal:** Demo video on hero CTA
- **Client Logos:** Real logos in social proof section
