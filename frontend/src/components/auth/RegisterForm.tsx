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
    <AuthCard
      title="Crie sua conta"
      footerText="Já tem conta?"
      footerLink={{
        href: '/auth/login',
        text: 'Faça login',
      }}
    >
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
        {error && (
          <div
            role="alert"
            className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800"
          >
            {error}
          </div>
        )}

        <Input
          id="workspaceName"
          label="Nome do Workspace"
          type="text"
          autoComplete="organization"
          placeholder="Meu Workspace"
          error={errors.workspaceName?.message}
          {...register('workspaceName')}
        />

        <Input
          id="fullName"
          label="Nome Completo"
          type="text"
          autoComplete="name"
          placeholder="Seu Nome"
          error={errors.fullName?.message}
          {...register('fullName')}
        />

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
              autoComplete="new-password"
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

        <Input
          id="confirmPassword"
          label="Confirmar Senha"
          type={showPassword ? 'text' : 'password'}
          autoComplete="new-password"
          placeholder="••••••••"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />

        <Button type="submit" loading={isLoading} className="w-full">
          {isLoading ? 'Criando conta...' : 'Criar Conta'}
        </Button>
      </form>
    </AuthCard>
  );
}
