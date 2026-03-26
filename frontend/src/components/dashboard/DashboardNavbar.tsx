'use client';

import Link from 'next/link';
import { useAuth } from '@/hooks/useAuth';
import useSessionStore from '@/stores/useSession';

export function DashboardNavbar() {
  const { user } = useSessionStore();
  const { logout } = useAuth();

  return (
    <nav className="border-b border-secondary-200 bg-white">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
        {/* Logo & Navigation Links */}
        <div className="flex items-center gap-8">
          <Link
            href="/dashboard"
            className="text-xl font-bold text-primary-600"
          >
            ScopeFlow
          </Link>
          <div className="hidden gap-6 sm:flex">
            <Link
              href="/dashboard"
              className="text-sm text-secondary-600 transition hover:text-secondary-900"
            >
              Dashboard
            </Link>
            <Link
              href="/dashboard/proposals"
              className="text-sm text-secondary-600 transition hover:text-secondary-900"
            >
              Propostas
            </Link>
            <Link
              href="/dashboard/briefings"
              className="text-sm text-secondary-600 transition hover:text-secondary-900"
            >
              Briefings
            </Link>
          </div>
        </div>

        {/* User Info & Logout */}
        <div className="flex items-center gap-4">
          {user && (
            <div className="hidden text-right sm:block">
              <p className="text-sm font-medium text-secondary-900">
                {user.fullName}
              </p>
              <p className="text-xs text-secondary-500">{user.email}</p>
            </div>
          )}
          <button
            onClick={logout}
            className="rounded-lg bg-red-600 px-4 py-1.5 text-sm font-medium text-white transition hover:bg-red-700"
          >
            Sair
          </button>
        </div>
      </div>
    </nav>
  );
}
