CREATE TABLE orders (
    id          BIGINT         PRIMARY KEY,
    customer_id BIGINT       NOT NULL,
    product     VARCHAR(255) NOT NULL,
    quantity    INTEGER      NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL
);