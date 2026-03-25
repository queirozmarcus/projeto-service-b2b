'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';
import useSessionStore from '@/stores/useSession';

const registerSchema = z
  .object({
    workspaceName: z
      .string()
      .min(3, 'Workspace precisa ter pelo menos 3 caracteres'),
    fullName: z.string().min(3, 'Nome precisa ter pelo menos 3 caracteres'),
    email: z.string().email('Email inválido'),
    password: z.string().min(8, 'Senha precisa ter pelo menos 8 caracteres'),
    confirmPassword: z.string().min(1, 'Confirme sua senha'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Senhas não conferem',
    path: ['confirmPassword'],
  });

type RegisterFormData = z.infer<typeof registerSchema>;

export function RegisterForm() {
  const [showPassword, setShowPassword] = useState(false);
  const { register: registerUser } = useAuth();
  const { isLoading, error } = useSessionStore();
  const router = useRouter();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (data: RegisterFormData) => {
    try {
      await registerUser(data);
      router.push('/dashboard');
    } catch {
      // Erro já disponível em useSessionStore.error
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-secondary-50 py-12">
      <div className="w-full max-w-md space-y-8 rounded-xl border border-secondary-200 bg-white p-8 shadow-sm">
        <div>
          <h1 className="text-center text-2xl font-bold text-primary-600">
            ScopeFlow
          </h1>
          <h2 className="mt-4 text-center text-xl font-semibold text-secondary-900">
            Crie sua conta
          </h2>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
          {error && (
            <div
              role="alert"
              className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800"
            >
              {error}
            </div>
          )}

          {/* Workspace */}
          <div>
            <label
              htmlFor="workspaceName"
              className="mb-1 block text-sm font-medium text-secondary-700"
            >
              Nome do Workspace
            </label>
            <input
              id="workspaceName"
              type="text"
              autoComplete="organization"
              {...register('workspaceName')}
              className={`w-full rounded-lg border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 ${
                errors.workspaceName
                  ? 'border-red-400 bg-red-50'
                  : 'border-secondary-300'
              }`}
            />
            {errors.workspaceName && (
              <p className="mt-1 text-xs text-red-600">
                {errors.workspaceName.message}
              </p>
            )}
          </div>

          {/* Nome completo */}
          <div>
            <label
              htmlFor="fullName"
              className="mb-1 block text-sm font-medium text-secondary-700"
            >
              Nome Completo
            </label>
            <input
              id="fullName"
              type="text"
              autoComplete="name"
              {...register('fullName')}
              className={`w-full rounded-lg border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 ${
                errors.fullName
                  ? 'border-red-400 bg-red-50'
                  : 'border-secondary-300'
              }`}
            />
            {errors.fullName && (
              <p className="mt-1 text-xs text-red-600">
                {errors.fullName.message}
              </p>
            )}
          </div>

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
                autoComplete="new-password"
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

          {/* Confirmar senha */}
          <div>
            <label
              htmlFor="confirmPassword"
              className="mb-1 block text-sm font-medium text-secondary-700"
            >
              Confirmar Senha
            </label>
            <input
              id="confirmPassword"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              {...register('confirmPassword')}
              className={`w-full rounded-lg border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 ${
                errors.confirmPassword
                  ? 'border-red-400 bg-red-50'
                  : 'border-secondary-300'
              }`}
            />
            {errors.confirmPassword && (
              <p className="mt-1 text-xs text-red-600">
                {errors.confirmPassword.message}
              </p>
            )}
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full rounded-lg bg-primary-600 py-2.5 text-sm font-semibold text-white hover:bg-primary-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isLoading ? 'Criando conta...' : 'Criar Conta'}
          </button>
        </form>

        <p className="text-center text-sm text-secondary-600">
          Já tem conta?{' '}
          <Link href="/auth/login" className="text-primary-600 hover:underline">
            Faça login
          </Link>
        </p>
      </div>
    </div>
  );
}
