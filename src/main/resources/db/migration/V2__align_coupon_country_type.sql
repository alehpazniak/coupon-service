ALTER TABLE coupons
    ALTER COLUMN country TYPE VARCHAR(2)
    USING country::VARCHAR(2);

ALTER TABLE coupons
    ADD CONSTRAINT chk_country_code CHECK (country ~ '^[A-Z]{2}$');
