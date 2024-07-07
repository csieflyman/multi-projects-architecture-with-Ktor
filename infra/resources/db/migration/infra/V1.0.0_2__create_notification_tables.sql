CREATE TABLE infra_device_push_token
(
    device_id varchar(64) NOT NULL primary key,
    user_id    uuid         NOT NULL,
    os         smallint     NOT NULL,
    push_token varchar(255) NOT NULL,
    created_at timestamp    NOT NULL DEFAULT now(),
    updated_at timestamp    NOT NULL
);

create index infra_device_push_token_user_id_idx on infra_device_push_token (user_id);

-- ########################## Trigger ##########################
DO
$$
    DECLARE
        t          text;
        tableArray varchar[] := array ['infra_device_push_token'];
    BEGIN
        FOREACH t IN ARRAY tableArray
            LOOP
                EXECUTE format('CREATE TRIGGER updated_at_trigger
                                BEFORE UPDATE ON %I
                                FOR EACH ROW EXECUTE PROCEDURE updated_at_trigger_func()', t, t);
            END loop;
    END;
$$ language 'plpgsql';