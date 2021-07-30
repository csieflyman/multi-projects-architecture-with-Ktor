CREATE FUNCTION updated_at_trigger_func() RETURNS trigger
    LANGUAGE plpgsql AS
$$BEGIN
    NEW.updated_at := current_timestamp;
RETURN NEW;
END;$$;