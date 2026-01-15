-- Удаление полей CDEK из таблицы orders
ALTER TABLE orders DROP COLUMN IF EXISTS cdek_pvz_code;
ALTER TABLE orders DROP COLUMN IF EXISTS cdek_pvz_address;
ALTER TABLE orders DROP COLUMN IF EXISTS cdek_city_code;
ALTER TABLE orders DROP COLUMN IF EXISTS cdek_tariff_code;
ALTER TABLE orders DROP COLUMN IF EXISTS cdek_delivery_sum;
ALTER TABLE orders DROP COLUMN IF EXISTS cdek_period_min;
ALTER TABLE orders DROP COLUMN IF EXISTS cdek_period_max;

-- Добавление полей Yandex Delivery в таблицу orders
ALTER TABLE orders ADD COLUMN IF NOT EXISTS yandex_pickup_point_id VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS yandex_pickup_point_address VARCHAR(500);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS yandex_pickup_point_name VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS yandex_latitude DOUBLE PRECISION;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS yandex_longitude DOUBLE PRECISION;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS yandex_delivery_price DECIMAL(10, 2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS yandex_delivery_term INTEGER;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS yandex_pickup_point_type VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS yandex_work_schedule VARCHAR(500);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS yandex_phone VARCHAR(50);

-- Создание индексов для оптимизации запросов
CREATE INDEX IF NOT EXISTS idx_order_yandex_pickup_point ON orders(yandex_pickup_point_id);
