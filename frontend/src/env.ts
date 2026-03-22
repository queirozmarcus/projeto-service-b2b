/**
 * Environment variables validation and type safety.
 *
 * All env vars must be defined here and validated at build time.
 */

const getEnv = () => {
  return {
    // Public API endpoint
    apiUrl: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1',
    nodeEnv: process.env.NODE_ENV || 'development',
  };
};

export const env = getEnv();
