CREATE TABLE infra_app_release
(
    app_id       varchar(30) NOT NULL,
    os           smallint    NOT NULL,
    ver_name     varchar(6)  NOT NULL,
    ver_num      smallint    NOT NULL,
    enabled      bool        NOT NULL,
    released_at  timestamp   NOT NULL,
    force_update bool        NOT NULL,
    created_at   timestamp   NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL,
    PRIMARY KEY (app_id, os, ver_name)
);

/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

-- ########################## Trigger ##########################
DO
$$
    DECLARE
        t          text;
        tableArray varchar[] := array ['infra_app_release'];
    BEGIN
        FOREACH t IN ARRAY tableArray
            LOOP
                EXECUTE format('CREATE TRIGGER updated_at_trigger
                                BEFORE UPDATE ON %I
                                FOR EACH ROW EXECUTE PROCEDURE updated_at_trigger_func()', t, t);
            END loop;
    END;
$$ language 'plpgsql';