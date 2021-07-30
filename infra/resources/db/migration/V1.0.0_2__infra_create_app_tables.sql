CREATE TABLE infra_app_release
(
    id int2 generated always as identity primary key,
    app_id varchar(30) NOT NULL,
    os int2 NOT NULL,
    ver_name varchar(6) NOT NULL,
    ver_num int2 NOT NULL,
    enabled bool NOT NULL,
    released_at timestamp NOT NULL,
    force_update int2 NOT NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL
);

create unique index infra_app_release_app_id_ver_name_idx on infra_app_release (app_id,ver_name);
create unique index infra_app_release_app_id_ver_num_idx on infra_app_release (app_id,ver_num);
create index infra_app_release_released_at_idx on infra_app_release (released_at);

CREATE TABLE infra_user_device (
    id uuid primary key,
    user_id uuid NOT NULL,
    source_type varchar(20) NOT NULL,
    enabled bool NOT NULL,
    push_token varchar(255),
    os_version varchar(200),
    user_agent varchar(200),
    enabled_at timestamp NOT NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL
);

create index infra_user_device_user_id_idx on infra_user_device (user_id);
create index infra_user_device_source_type_idx on infra_user_device (source_type);
create unique index infra_user_device_push_token_idx on infra_user_device (push_token);

-- ########################## Trigger ##########################
DO $$
    DECLARE
        t text;
tableArray varchar[] := array['infra_app_release', 'infra_user_device'];
BEGIN
    FOREACH t IN ARRAY tableArray
        LOOP
            EXECUTE format('CREATE TRIGGER updated_at_trigger
                                BEFORE UPDATE ON %I
                                FOR EACH ROW EXECUTE PROCEDURE updated_at_trigger_func()', t,t);
        END loop;
END;
$$ language 'plpgsql';