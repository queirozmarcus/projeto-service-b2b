'use client';

import { useEffect, useRef, useState } from 'react';

interface FeatureCardProps {
  icon: string;
  title: string;
  description: string;
  index?: number;
}

export function FeatureCard({
  icon,
  title,
  description,
  index = 0,
}: FeatureCardProps) {
  const [isInView, setIsInView] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsInView(true);
        }
      },
      { threshold: 0.1 }
    );

    if (ref.current) {
      observer.observe(ref.current);
    }

    return () => observer.disconnect();
  }, []);

  return (
    <div
      ref={ref}
      className={`group relative rounded-xl border border-secondary-200 bg-gradient-to-br from-white to-secondary-50 p-8 transition-all duration-500 hover:border-primary-300 hover:shadow-lg ${
        isInView ? 'animate-slide-up' : 'opacity-0'
      }`}
      style={{
        animationDelay: isInView ? `${index * 100}ms` : '0ms',
      }}
    >
      {/* Glow effect on hover */}
      <div className="absolute inset-0 rounded-xl bg-gradient-to-br from-primary-500 to-transparent opacity-0 transition-opacity duration-300 group-hover:opacity-5" />

      {/* Icon background */}
      <div className="mb-6 inline-flex rounded-lg bg-gradient-to-br from-primary-100 to-primary-50 p-3 text-3xl transition-all duration-300 group-hover:scale-110 group-hover:from-primary-200 group-hover:to-primary-100">
        {icon}
      </div>

      {/* Content */}
      <div className="relative z-10 space-y-3">
        <h3 className="font-display text-xl font-700 text-secondary-900">
          {title}
        </h3>
        <p className="text-secondary-600 leading-relaxed">{description}</p>
      </div>

      {/* Bottom accent */}
      <div className="absolute bottom-0 left-0 h-1 w-0 rounded-full bg-gradient-to-r from-primary-600 to-primary-400 transition-all duration-300 group-hover:w-16" />
    </div>
  );
}
