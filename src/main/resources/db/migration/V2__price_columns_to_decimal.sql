-- Convert money columns from FLOAT to DECIMAL(10,2).
--
-- FLOAT stores an approximation and produces the classic 0.1 + 0.2 != 0.3
-- rounding errors when accumulating order totals. Money must be exact:
-- DECIMAL(10,2) fixes the scale to two fractional digits and stores the
-- value as an integer under the hood, so arithmetic is precise up to the
-- declared precision.
--
-- (10,2) allows values up to 99,999,999.99 which is more than enough for
-- consumer prices. Existing FLOAT values are rounded to two decimals by
-- MySQL on MODIFY; this is a one-way lossy conversion but no meaningful
-- precision is lost for prices.

ALTER TABLE product
    MODIFY COLUMN price DECIMAL(10, 2) DEFAULT NULL;

ALTER TABLE orders
    MODIFY COLUMN price_sum DECIMAL(10, 2) DEFAULT NULL;
