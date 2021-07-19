CREATE TABLE ops_user (
   id uuid primary key,
   account varchar(64) NOT NULL,
   enabled bool NOT NULL,
   role varchar(20) NOT NULL,
   name varchar(30) NOT NULL,
   email varchar(64) NOT NULL,
   mobile char(10),
   lang varchar(20),
   password varchar(1000) NOT NULL,
   created_at timestamp NOT NULL DEFAULT now(),
   updated_at timestamp NOT NULL
);

create unique index ops_user_account_idx on ops_user (account);
create index ops_user_password_idx on ops_user (password);
create index ops_user_created_at_idx on ops_user (created_at);

-- ########################## Trigger ##########################
DO $$
    DECLARE
        t text;
tableArray varchar[] := array['ops_user'];
BEGIN
    FOREACH t IN ARRAY tableArray
        LOOP
EXECUTE format('CREATE TRIGGER updated_at_trigger
                                BEFORE UPDATE ON %I
                                FOR EACH ROW EXECUTE PROCEDURE updated_at_trigger_func()', t,t);
END loop;
END;
$$ language 'plpgsql';