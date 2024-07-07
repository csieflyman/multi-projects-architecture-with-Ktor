CREATE TABLE infra_app_release
(
    id           bigint generated always as identity primary key,
    app_id       varchar(30) NOT NULL,
    os           smallint    NOT NULL,
    ver_name     varchar(6)  NOT NULL,
    ver_num      smallint    NOT NULL,
    enabled      bool        NOT NULL,
    released_at  timestamp   NOT NULL,
    force_update bool        NOT NULL,
    created_at   timestamp   NOT NULL DEFAULT now(),
    updated_at   timestamp   NOT NULL
);

create unique index infra_app_release_app_id_ver_name_idx on infra_app_release (app_id, os, ver_name);
create index infra_app_release_released_at_idx on infra_app_release (released_at);

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