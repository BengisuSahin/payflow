-- Schema as it existed when Flyway was introduced (previously created by Hibernate).

CREATE TABLE payment_orders (
    id              uuid                        NOT NULL,
    version         bigint                      NOT NULL DEFAULT 0,
    customer_id     varchar(64)                 NOT NULL,
    amount          numeric(19, 4)              NOT NULL,
    currency        varchar(3)                  NOT NULL,
    status          varchar(32)                 NOT NULL,
    idempotency_key varchar(255),
    created_at      timestamp(6) with time zone NOT NULL,
    CONSTRAINT payment_orders_pkey PRIMARY KEY (id),
    -- NULL keys never collide in Postgres, so requests without a key are unaffected.
    CONSTRAINT uk_payment_orders_customer_idempotency_key UNIQUE (customer_id, idempotency_key)
);

CREATE TABLE outbox_events (
    id           uuid                        NOT NULL,
    aggregate_id uuid                        NOT NULL,
    event_type   varchar(255)                NOT NULL,
    payload      text                        NOT NULL,
    created_at   timestamp(6) with time zone NOT NULL,
    published_at timestamp(6) with time zone,
    CONSTRAINT outbox_events_pkey PRIMARY KEY (id)
);
