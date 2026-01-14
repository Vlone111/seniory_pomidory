-- Add shop_id column to categories table
ALTER TABLE categories ADD COLUMN shop_id UUID;

-- Create index for faster queries
CREATE INDEX idx_category_shop_id ON categories(shop_id);

-- Add shop_id column to brands table
ALTER TABLE brands ADD COLUMN shop_id UUID;

-- Remove unique constraint on brand name (brands are now shop-scoped)
ALTER TABLE brands DROP CONSTRAINT IF EXISTS brands_name_key;

-- Create index for faster queries
CREATE INDEX idx_brand_shop_id ON brands(shop_id);
