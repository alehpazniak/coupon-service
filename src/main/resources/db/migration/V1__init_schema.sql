CREATE TABLE coupons
(
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code           VARCHAR(255) NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    max_usages     INTEGER      NOT NULL,
    current_usages INTEGER      NOT NULL    DEFAULT 0,
    country        CHAR(2)      NOT NULL,
    version        BIGINT       NOT NULL    DEFAULT 0,

    CONSTRAINT uk_coupons_code      UNIQUE (code),
    CONSTRAINT chk_max_usages       CHECK (max_usages > 0),
    CONSTRAINT chk_current_usages   CHECK (current_usages >= 0),
    CONSTRAINT chk_usages_not_exceeded CHECK (current_usages <= max_usages)
);

CREATE INDEX idx_coupons_code ON coupons (code);

CREATE TABLE coupon_usages
(
    id        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    coupon_id UUID         NOT NULL REFERENCES coupons (id),
    user_id   VARCHAR(255) NOT NULL,
    used_at   TIMESTAMP    NOT NULL,

    CONSTRAINT uk_coupon_usages_coupon_user UNIQUE (coupon_id, user_id)
);

CREATE INDEX idx_coupon_usages_lookup ON coupon_usages (coupon_id, user_id);
