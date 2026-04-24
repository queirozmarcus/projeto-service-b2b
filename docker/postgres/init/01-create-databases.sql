-- Init script executado pelo container postgres na primeira inicialização
-- O database 'scopeflow' já é criado automaticamente via POSTGRES_DB env var
-- Este script garante que 'scopeflow_users' também exista no mesmo container (staging unificado)
SELECT 'CREATE DATABASE scopeflow_users'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'scopeflow_users')\gexec
