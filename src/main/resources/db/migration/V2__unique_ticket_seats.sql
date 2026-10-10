-- Prevent duplicate inventory positions without modifying the already-applied V1.
-- Preflight: reject existing collisions; do not silently delete or rewrite paid tickets.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM tickets
        WHERE session_id IS NOT NULL AND fila IS NOT NULL AND seat IS NOT NULL
        GROUP BY session_id, fila, seat
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate ticket seats found. Inspect tickets grouped by session_id, fila, seat before applying V2';
    END IF;
END
$$;

-- Legacy tickets may have nullable coordinates; PostgreSQL UNIQUE permits NULLs.
-- Every fully specified seat is nevertheless protected for its session.
ALTER TABLE tickets
    ADD CONSTRAINT uq_tickets_session_row_seat UNIQUE (session_id, fila, seat);
