CREATE SCHEMA IF NOT EXISTS orders;
ALTER TABLE IF EXISTS orders.orders ADD COLUMN IF NOT EXISTS requirements varchar(2000);
ALTER TABLE IF EXISTS orders.custom_offers ADD COLUMN IF NOT EXISTS order_id bigint;
