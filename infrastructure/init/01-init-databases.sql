-- Create separate users for each product
CREATE ROLE payments_user
    WITH LOGIN PASSWORD 'payments_dev_password';

CREATE ROLE crypto_user
    WITH LOGIN PASSWORD 'crypto_dev_password';

-- Create separate databases
CREATE DATABASE payflow_payments
    OWNER payments_user;

CREATE DATABASE payflow_crypto
    OWNER crypto_user;
