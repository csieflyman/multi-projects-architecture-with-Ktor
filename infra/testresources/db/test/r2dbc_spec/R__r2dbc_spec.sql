CREATE TABLE r2dbc_test_user
(
    id         uuid primary key,
    account    varchar(64) NOT NULL,
    enabled    bool        NOT NULL,
    gender     int2,
    birth_year int2,
    created_at timestamp   NOT NULL DEFAULT now(),
    updated_at timestamp   NOT NULL
);