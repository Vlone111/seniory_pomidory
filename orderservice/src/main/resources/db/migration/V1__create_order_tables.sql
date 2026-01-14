-- Create delivery_methods table
CREATE TABLE delivery_methods (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    price DECIMAL(10, 2) NOT NULL
);

-- Create payment_methods table
CREATE TABLE payment_methods (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    price DECIMAL(10, 2) NOT NULL
);

-- Create orders table
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    recipient_id UUID NOT NULL,
    delivery_method_id UUID NOT NULL,
    payment_method_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_delivery_method FOREIGN KEY (delivery_method_id) REFERENCES delivery_methods(id),
    CONSTRAINT fk_payment_method FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id)
);

-- Create order_items table
CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    shop_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    price_per_item DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_order_user_id ON orders(user_id);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_order_created_at ON orders(created_at);
CREATE INDEX idx_order_item_order_id ON order_items(order_id);
CREATE INDEX idx_order_item_product_id ON order_items(product_id);

-- Insert default delivery methods
INSERT INTO delivery_methods (id, name, price) VALUES
    (gen_random_uuid(), 'Courier', 5.00),
    (gen_random_uuid(), 'Self-pickup', 0.00),
    (gen_random_uuid(), 'Express Delivery', 10.00);

-- Insert default payment methods
INSERT INTO payment_methods (id, name, price) VALUES
    (gen_random_uuid(), 'Card', 0.00),
    (gen_random_uuid(), 'Cash', 0.00),
    (gen_random_uuid(), 'Online Payment', 0.00);
