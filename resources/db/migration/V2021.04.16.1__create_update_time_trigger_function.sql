CREATE FUNCTION update_time_trigger_func() RETURNS trigger
    LANGUAGE plpgsql AS
$$BEGIN
    NEW.update_time := current_timestamp;
RETURN NEW;
END;$$;