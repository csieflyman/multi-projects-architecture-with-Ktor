CREATE TABLE club_user (
   id uuid primary key,
   account varchar(64) NOT NULL,
   name varchar(30) NOT NULL,
   age int2,
   enabled bool NOT NULL,
   role int2 NOT NULL,
   password varchar(1000) NOT NULL,
   create_time timestamp NOT NULL DEFAULT now(),
   update_time timestamp NOT NULL
);

create unique index club_user_account_idx on club_user (account);
create index club_user_password_idx on club_user (password);
create index club_user_create_time_idx on club_user (create_time);

-- ########################## Trigger ##########################
DO $$
    DECLARE
        t text;
tableArray varchar[] := array['club_user'];
BEGIN
    FOREACH t IN ARRAY tableArray
        LOOP
EXECUTE format('CREATE TRIGGER update_time_trigger
                                BEFORE UPDATE ON %I
                                FOR EACH ROW EXECUTE PROCEDURE update_time_trigger_func()', t,t);
END loop;
END;
$$ language 'plpgsql';