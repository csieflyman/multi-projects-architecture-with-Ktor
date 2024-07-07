CREATE TABLE "user"
(
    id         uuid primary key,
    account    varchar(64)   NOT NULL,
    enabled    bool          NOT NULL,
    name       varchar(30)   NOT NULL,
    email      varchar(64)   NOT NULL,
    mobile varchar(15),
    lang       varchar(20),
    password   varchar(1000) NOT NULL,
    roles      varchar(100)  NOT NULL,
    created_at timestamp     NOT NULL DEFAULT now(),
    updated_at timestamp     NOT NULL
);

create unique index user_account_idx on "user" (account);
create index user_created_at_idx on "user" (created_at);

-- ########################## Trigger ##########################
DO
$$
    DECLARE
        t          text;
        tableArray varchar[] := array ['user'];
    BEGIN
        FOREACH t IN ARRAY tableArray
            LOOP
                EXECUTE format('CREATE TRIGGER updated_at_trigger
                                BEFORE UPDATE ON %I
                                FOR EACH ROW EXECUTE PROCEDURE updated_at_trigger_func()', t, t);
            END loop;
    END;
$$ language 'plpgsql';