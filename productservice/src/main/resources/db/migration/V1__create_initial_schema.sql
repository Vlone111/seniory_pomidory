-- Create brands table
CREATE TABLE brands (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    logo_url VARCHAR(500)
);

-- Create categories table
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    parent_id UUID,
    FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- Create sizes table
CREATE TABLE sizes (
    id UUID PRIMARY KEY,
    value VARCHAR(50) NOT NULL
);

-- Create products table
CREATE TABLE products (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(19, 2) NOT NULL,
    category_id UUID,
    brand_id UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    FOREIGN KEY (brand_id) REFERENCES brands(id) ON DELETE SET NULL
);

-- Create index on shop_id for faster queries
CREATE INDEX idx_shop_id ON products(shop_id);

-- Create product_images table (ElementCollection)
CREATE TABLE product_images (
    product_id UUID NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Create product_sizes table (Inventory)
CREATE TABLE product_sizes (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    size_id UUID NOT NULL,
    quantity_available INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (size_id) REFERENCES sizes(id) ON DELETE CASCADE,
    UNIQUE (product_id, size_id)
);
