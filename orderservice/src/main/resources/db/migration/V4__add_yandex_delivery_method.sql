-- Добавляем колонку description если ее еще нет
ALTER TABLE delivery_methods ADD COLUMN IF NOT EXISTS description VARCHAR(500);

-- Добавляем метод доставки "Яндекс Доставка (ПВЗ)" если его еще нет
INSERT INTO delivery_methods (id, name, price, description)
SELECT 
    gen_random_uuid(), 
    'Яндекс Доставка (ПВЗ)', 
    0.00,
    'Доставка в пункты выдачи заказов Яндекс. Стоимость рассчитывается автоматически.'
WHERE NOT EXISTS (
    SELECT 1 FROM delivery_methods WHERE name LIKE '%Яндекс%' OR name LIKE '%ПВЗ%'
);

-- Обновляем существующие методы доставки для добавления описания, если его нет
UPDATE delivery_methods 
SET description = 'Доставка курьером до двери' 
WHERE name = 'Courier' AND (description IS NULL OR description = '');

UPDATE delivery_methods 
SET description = 'Самостоятельный вывоз из пункта выдачи' 
WHERE name = 'Self-pickup' AND (description IS NULL OR description = '');

UPDATE delivery_methods 
SET description = 'Экспресс доставка курьером в течение дня' 
WHERE name = 'Express Delivery' AND (description IS NULL OR description = '');
