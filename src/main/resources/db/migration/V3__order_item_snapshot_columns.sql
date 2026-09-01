-- Store the product name and unit price at order time on each order_item.
--
-- Without this snapshot, /api/order/{id} joins to product at read time
-- and shows the *current* name and price. If a product is renamed,
-- repriced, or deleted after the order was placed, historical orders
-- would silently change — unacceptable for receipts, accounting, tax
-- filings, and customer support.
--
-- Existing rows (none in prod today) get NULL snapshots; new orders
-- always populate both. A later PR may backfill or drop old rows.

ALTER TABLE order_item
    ADD COLUMN product_name VARCHAR(255) DEFAULT NULL,
    ADD COLUMN unit_price   DECIMAL(10, 2) DEFAULT NULL;
