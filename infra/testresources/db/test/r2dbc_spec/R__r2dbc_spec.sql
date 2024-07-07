CREATE TABLE r2dbc_test_user
(
    id         uuid primary key,
    account    varchar(64) NOT NULL,
    enabled    bool        NOT NULL,
    gender varchar(10),
    birth_year smallint,
    created_at timestamp   NOT NULL DEFAULT now(),
    updated_at timestamp   NOT NULL
);