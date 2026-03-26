'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  Squares2X2Icon,
  DocumentTextIcon,
  ClipboardDocumentListIcon,
  ArrowLeftEndOnRectangleIcon,
} from '@heroicons/react/24/outline';
import { useAuth } from '@/hooks/useAuth';
import useSessionStore from '@/stores/useSession';

const navLinks = [
  { href: '/dashboard', label: 'Dashboard', icon: Squares2X2Icon },
  { href: '/dashboard/proposals', label: 'Propostas', icon: DocumentTextIcon },
  { href: '/dashboard/briefings', label: 'Briefings', icon: ClipboardDocumentListIcon },
];

export function DashboardNavbar() {
  const { user } = useSessionStore();
  const { logout } = useAuth();
  const pathname = usePathname();

  return (
    <nav className="border-b border-secondary-200 bg-surface">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-3.5">

        {/* Logo + nav links */}
        <div className="flex items-center gap-8">
          <Link href="/dashboard" className="flex items-center gap-2.5">
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary-600">
              <svg className="h-3.5 w-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2.5}
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"
                />
              </svg>
            </div>
            <span className="text-base font-bold text-ink-900">ScopeFlow</span>
          </Link>

          <div className="hidden items-center gap-1 sm:flex">
            {navLinks.map(({ href, label, icon: Icon }) => {
              const active = pathname === href;
              return (
                <Link
                  key={href}
                  href={href}
                  className={`flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                    active
                      ? 'bg-primary-50 text-primary-700'
                      : 'text-secondary-600 hover:bg-secondary-50 hover:text-ink-800'
                  }`}
                >
                  <Icon className="h-4 w-4" />
                  {label}
                </Link>
              );
            })}
          </div>
        </div>

        {/* User + logout */}
        <div className="flex items-center gap-4">
          {user && (
            <div className="hidden text-right sm:block">
              <p className="text-sm font-semibold text-ink-800">{user.fullName}</p>
              <p className="text-xs text-secondary-500">{user.email}</p>
            </div>
          )}
          <button
            onClick={logout}
            className="flex items-center gap-1.5 rounded-lg border border-secondary-200 px-3 py-1.5 text-sm font-medium text-secondary-600 transition-all hover:border-red-200 hover:bg-red-50 hover:text-red-700"
          >
            <ArrowLeftEndOnRectangleIcon className="h-4 w-4" />
            Sair
          </button>
        </div>
      </div>
    </nav>
  );
}
