-- Users table (id matches Keycloak user ID)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Recipients (delivery addresses) table
CREATE TABLE IF NOT EXISTS recipients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    address VARCHAR(500) NOT NULL,
    zip_code VARCHAR(20),
    city VARCHAR(100),
    comment VARCHAR(500)
);

-- Index for faster lookups by user_id
CREATE INDEX IF NOT EXISTS idx_recipients_user_id ON recipients(user_id);
