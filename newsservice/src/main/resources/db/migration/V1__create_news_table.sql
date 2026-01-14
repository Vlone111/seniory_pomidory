-- Create news table
CREATE TABLE news (
    id UUID PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    slug VARCHAR(500) NOT NULL UNIQUE,
    preview_image_url VARCHAR(500),
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Create indexes for better query performance
CREATE INDEX idx_slug ON news(slug) WHERE deleted = false;
CREATE INDEX idx_published ON news(is_published, published_at DESC) WHERE deleted = false AND is_published = true;
CREATE INDEX idx_created_at ON news(created_at DESC) WHERE deleted = false;
CREATE INDEX idx_deleted ON news(deleted);
