'use client';

import { useEffect, useRef, useState, ReactNode } from 'react';

interface FeatureCardProps {
  icon: ReactNode;
  title: string;
  description: string;
  index?: number;
  size?: 'default' | 'large';
  accentColor?: 'primary' | 'accent' | 'indigo';
}

const accentMap = {
  primary: {
    iconBg: 'bg-primary-50',
    iconBorder: 'border-primary-100',
    iconColor: 'text-primary-600',
    hoverBorder: 'hover:border-primary-300',
    bar: 'bg-primary-500',
  },
  accent: {
    iconBg: 'bg-accent-50',
    iconBorder: 'border-accent-100',
    iconColor: 'text-accent-600',
    hoverBorder: 'hover:border-accent-300',
    bar: 'bg-accent-500',
  },
  indigo: {
    iconBg: 'bg-indigo-50',
    iconBorder: 'border-indigo-100',
    iconColor: 'text-indigo-600',
    hoverBorder: 'hover:border-indigo-300',
    bar: 'bg-indigo-500',
  },
};

export function FeatureCard({
  icon,
  title,
  description,
  index = 0,
  size = 'default',
  accentColor = 'primary',
}: FeatureCardProps) {
  const [isInView, setIsInView] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const colors = accentMap[accentColor];

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsInView(true);
        }
      },
      { threshold: 0.1 }
    );

    if (ref.current) observer.observe(ref.current);
    return () => observer.disconnect();
  }, []);

  return (
    <div
      ref={ref}
      className={`group relative rounded-2xl border border-secondary-200 bg-white p-8 transition-all duration-300 ${colors.hoverBorder} hover:shadow-card-hover hover:-translate-y-0.5 ${
        isInView ? 'animate-slide-up' : 'opacity-0'
      } ${size === 'large' ? 'lg:p-10' : ''}`}
      style={{ animationDelay: isInView ? `${index * 120}ms` : '0ms' }}
    >
      {/* Icon */}
      <div className={`mb-6 inline-flex rounded-xl border p-3 ${colors.iconBg} ${colors.iconBorder} ${colors.iconColor} transition-transform duration-300 group-hover:scale-110`}>
        {icon}
      </div>

      {/* Content */}
      <div className="space-y-3">
        <h3 className={`font-display font-bold text-ink-800 ${size === 'large' ? 'text-2xl' : 'text-xl'}`}>
          {title}
        </h3>
        <p className={`leading-relaxed text-secondary-600 ${size === 'large' ? 'text-base' : 'text-sm'}`}>
          {description}
        </p>
      </div>

      {/* Bottom accent bar — reveals on hover */}
      <div className={`absolute bottom-0 left-8 right-8 h-0.5 scale-x-0 rounded-full ${colors.bar} transition-transform duration-300 origin-left group-hover:scale-x-100`} />
    </div>
  );
}
