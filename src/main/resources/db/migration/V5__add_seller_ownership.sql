INSERT INTO roles (name)
SELECT 'ROLE_SELLER'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE name = 'ROLE_SELLER'
);

ALTER TABLE product
    ADD COLUMN seller_id BIGINT NULL,
    ADD KEY ix_product_seller (seller_id),
    ADD CONSTRAINT fk_product_seller FOREIGN KEY (seller_id) REFERENCES users (id);

ALTER TABLE order_item
    ADD COLUMN seller_id BIGINT NULL,
    ADD KEY ix_order_item_seller (seller_id),
    ADD CONSTRAINT fk_order_item_seller FOREIGN KEY (seller_id) REFERENCES users (id);
