'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';
import useSessionStore from '@/stores/useSession';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { AuthCard } from '@/components/auth/AuthCard';

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
    <AuthCard
      title="Entrar na sua conta"
      footerText="Não tem conta?"
      footerLink={{
        href: '/auth/register',
        text: 'Crie uma aqui',
      }}
    >
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
        {error && (
          <div
            role="alert"
            data-testid="login-error"
            className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800"
          >
            {error}
          </div>
        )}

        <Input
          id="email"
          label="Email"
          type="email"
          autoComplete="email"
          placeholder="seu@email.com"
          error={errors.email?.message}
          {...register('email')}
        />

        <div className="space-y-1">
          <label
            htmlFor="password"
            className="block text-sm font-medium text-secondary-700"
          >
            Senha
          </label>
          <div className="relative">
            <input
              id="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              placeholder="••••••••"
              className={`w-full rounded-lg border px-3 py-2 pr-12 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 transition-colors ${
                errors.password
                  ? 'border-red-400 bg-red-50 focus:ring-red-500'
                  : 'border-secondary-300 focus:ring-primary-500'
              }`}
              {...register('password')}
            />
            <button
              type="button"
              onClick={() => setShowPassword((v) => !v)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-secondary-500 hover:text-secondary-800 transition-colors"
            >
              {showPassword ? 'Ocultar' : 'Mostrar'}
            </button>
          </div>
          {errors.password && (
            <p className="text-xs text-red-600" role="alert">
              {errors.password.message}
            </p>
          )}
        </div>

        <Button type="submit" loading={isLoading} className="w-full">
          {isLoading ? 'Entrando...' : 'Entrar'}
        </Button>
      </form>
    </AuthCard>
  );
}
