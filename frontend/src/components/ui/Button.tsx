import React, { forwardRef } from 'react';

interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  loading?: boolean | undefined;
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | undefined;
  size?: 'sm' | 'md' | 'lg' | undefined;
  children: React.ReactNode;
}

const variantStyles = {
  primary:
    'bg-primary-600 text-white hover:bg-primary-700 disabled:bg-primary-400',
  secondary:
    'bg-secondary-600 text-white hover:bg-secondary-700 disabled:bg-secondary-400',
  outline:
    'border border-secondary-300 text-secondary-900 hover:bg-secondary-50 disabled:opacity-50',
  ghost:
    'text-primary-600 hover:bg-primary-50 disabled:opacity-50',
};

const sizeStyles = {
  sm: 'px-3 py-1.5 text-xs',
  md: 'px-4 py-2 text-sm',
  lg: 'px-6 py-3 text-base',
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      loading = false,
      variant = 'primary',
      size = 'md',
      disabled = false,
      children,
      className = '',
      ...props
    },
    ref
  ) => {
    const baseStyles =
      'font-semibold rounded-lg transition-colors duration-200 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500';

    const styles = `${baseStyles} ${variantStyles[variant]} ${sizeStyles[size]} ${className}`;

    return (
      <button
        ref={ref}
        disabled={disabled || loading}
        className={styles}
        {...props}
      >
        {loading ? (
          <span className="flex items-center gap-2">
            <span
              className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent"
              role="status"
              aria-label="Loading"
            />
            Carregando...
          </span>
        ) : (
          children
        )}
      </button>
    );
  }
);

Button.displayName = 'Button';
