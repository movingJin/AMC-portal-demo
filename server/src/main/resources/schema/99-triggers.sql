-- updated_at 자동 갱신 트리거
CREATE OR REPLACE FUNCTION portal.set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    t TEXT;
BEGIN
    FOR t IN SELECT unnest(ARRAY['users', 'board_masters', 'boards', 'comments', 'projects']) LOOP
        EXECUTE format('
            DROP TRIGGER IF EXISTS trg_%s_updated_at ON portal.%s;
            CREATE TRIGGER trg_%s_updated_at BEFORE UPDATE ON portal.%s
                FOR EACH ROW EXECUTE FUNCTION portal.set_updated_at();
        ', t, t, t, t);
    END LOOP;
END$$;
