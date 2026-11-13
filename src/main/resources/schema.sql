-- =============================================
-- Script de creación de la tabla (PostgreSQL)
-- Gestión de Usuarios - Arquitectura Hexagonal
-- Se ejecuta automáticamente al arrancar (spring.sql.init.mode=always)
-- =============================================

CREATE TABLE IF NOT EXISTS users (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'MEMBER', 'REVIEWER')),
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'BLOCKED')),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Foto de perfil: key del objeto en el bucket privado de Backblaze B2 (Guia 9).
-- La imagen NO se guarda en la base de datos, solo su referencia.
-- ADD COLUMN IF NOT EXISTS mantiene el script idempotente para bases ya creadas.
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_photo_key VARCHAR(255) NULL;

-- Usuario administrador inicial (password: Admin1234!)
INSERT INTO users (id, name, email, password, role, status)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Administrador',
    'admin@example.com',
    '$2a$12$2LhOnh.0gBxsdVzub7aa4egk51GL0v5TZKFYS9dfkNK6WeRaMf/pq',
    'ADMIN',
    'ACTIVE'
)
ON CONFLICT (id) DO NOTHING;
