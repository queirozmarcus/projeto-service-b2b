'use client';

import Link from 'next/link';
import React from 'react';

interface AuthCardProps {
  children: React.ReactNode;
  title: string;
  description?: string;
  footerText?: string;
  footerLink?: {
    href: string;
    text: string;
  };
}

export function AuthCard({
  children,
  title,
  description,
  footerText,
  footerLink,
}: AuthCardProps) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-secondary-50 py-12 px-4">
      <div className="w-full max-w-md space-y-8 rounded-xl border border-secondary-200 bg-white p-8 shadow-sm">
        {/* Header */}
        <div className="space-y-2">
          <h1 className="text-center text-2xl font-bold text-primary-600">
            ScopeFlow
          </h1>
          <h2 className="text-center text-xl font-semibold text-secondary-900">
            {title}
          </h2>
          {description && (
            <p className="text-center text-sm text-secondary-600">
              {description}
            </p>
          )}
        </div>

        {/* Form */}
        {children}

        {/* Footer */}
        {footerLink && footerText && (
          <p className="text-center text-sm text-secondary-600">
            {footerText}{' '}
            <Link
              href={footerLink.href}
              className="text-primary-600 hover:underline"
            >
              {footerLink.text}
            </Link>
          </p>
        )}
      </div>
    </div>
  );
}
