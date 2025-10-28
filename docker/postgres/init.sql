-- Script de inicialização do PostgreSQL
-- Executado automaticamente na primeira criação do container

-- Criar extensões necessárias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Configurar timezone para São Paulo
SET timezone = 'America/Sao_Paulo';

-- Logar criação
DO $$
BEGIN
    RAISE NOTICE 'Database pagamentos inicializado com sucesso';
    RAISE NOTICE 'Extensões uuid-ossp e pg_trgm criadas';
    RAISE NOTICE 'Timezone: America/Sao_Paulo';
END $$;
