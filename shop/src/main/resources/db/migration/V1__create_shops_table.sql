-- Create shops table
CREATE TABLE shops (
    id UUID PRIMARY KEY,
    shop_name VARCHAR(255) NOT NULL UNIQUE,
    shop_url VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    pfp_url VARCHAR(500),
    design_code VARCHAR(255) NOT NULL,
    owner_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- Create index on owner_id for faster queries
CREATE INDEX idx_owner_id ON shops(owner_id);
