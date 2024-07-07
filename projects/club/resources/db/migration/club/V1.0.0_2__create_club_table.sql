CREATE TABLE club
(
    id varchar(30) NOT NULL primary key,
    name       varchar(30) NOT NULL,
    enabled    bool        NOT NULL,
    creator_id uuid        NOT NULL,
    created_at timestamp   NOT NULL DEFAULT now(),
    updated_at timestamp   NOT NULL
);

create index club_name_idx on club (name);

CREATE TABLE user_joined_club
(
    user_id    uuid        NOT NULL,
    club_id    varchar(30) NOT NULL,
    is_admin   boolean     NOT NULL,
    created_at timestamp   NOT NULL DEFAULT now(),
    updated_at timestamp   NOT NULL,
    PRIMARY KEY (user_id, club_id)
);

-- ########################## Trigger ##########################
DO
$$
    DECLARE
        t          text;
        tableArray varchar[] := array ['club', 'user_joined_club'];
    BEGIN
        FOREACH t IN ARRAY tableArray
            LOOP
                EXECUTE format('CREATE TRIGGER updated_at_trigger
                                BEFORE UPDATE ON %I
                                FOR EACH ROW EXECUTE PROCEDURE updated_at_trigger_func()', t, t);
            END loop;
    END;
$$ language 'plpgsql';