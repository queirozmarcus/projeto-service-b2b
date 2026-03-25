'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';
import useSessionStore from '@/stores/useSession';

const loginSchema = z.object({
  email: z.string().email('Email inválido'),
  password: z.string().min(1, 'Senha é obrigatória'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export function LoginForm() {
  const [showPassword, setShowPassword] = useState(false);
  const { login } = useAuth();
  const { isLoading, error } = useSessionStore();
  const router = useRouter();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    try {
      await login(data);
      router.push('/dashboard');
    } catch {
      // Erro já disponível em useSessionStore.error
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-secondary-50">
      <div className="w-full max-w-md space-y-8 rounded-xl border border-secondary-200 bg-white p-8 shadow-sm">
        <div>
          <h1 className="text-center text-2xl font-bold text-primary-600">
            ScopeFlow
          </h1>
          <h2 className="mt-4 text-center text-xl font-semibold text-secondary-900">
            Entrar na sua conta
          </h2>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
          {error && (
            <div
              role="alert"
              className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800"
            >
              {error}
            </div>
          )}

          {/* Email */}
          <div>
            <label
              htmlFor="email"
              className="mb-1 block text-sm font-medium text-secondary-700"
            >
              Email
            </label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              {...register('email')}
              className={`w-full rounded-lg border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 ${
                errors.email
                  ? 'border-red-400 bg-red-50'
                  : 'border-secondary-300'
              }`}
            />
            {errors.email && (
              <p className="mt-1 text-xs text-red-600">
                {errors.email.message}
              </p>
            )}
          </div>

          {/* Senha */}
          <div>
            <label
              htmlFor="password"
              className="mb-1 block text-sm font-medium text-secondary-700"
            >
              Senha
            </label>
            <div className="relative">
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                {...register('password')}
                className={`w-full rounded-lg border px-3 py-2 pr-16 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 ${
                  errors.password
                    ? 'border-red-400 bg-red-50'
                    : 'border-secondary-300'
                }`}
              />
              <button
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-secondary-500 hover:text-secondary-800"
              >
                {showPassword ? 'Ocultar' : 'Mostrar'}
              </button>
            </div>
            {errors.password && (
              <p className="mt-1 text-xs text-red-600">
                {errors.password.message}
              </p>
            )}
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full rounded-lg bg-primary-600 py-2.5 text-sm font-semibold text-white hover:bg-primary-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isLoading ? 'Entrando...' : 'Entrar'}
          </button>
        </form>

        <p className="text-center text-sm text-secondary-600">
          Não tem conta?{' '}
          <Link href="/auth/register" className="text-primary-600 hover:underline">
            Crie uma aqui
          </Link>
        </p>
      </div>
    </div>
  );
}
