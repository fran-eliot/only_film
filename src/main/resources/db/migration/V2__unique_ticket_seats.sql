-- Flyway runs against PostgreSQL and the H2 regression database.
-- A pre-deployment duplicate audit query is documented in
-- docs/FLYWAY_V2_SEAT_UNIQUENESS.md.
-- If duplicates exist, the UNIQUE constraint fails atomically: no tickets
-- are deleted, rewritten, or silently deduplicated.
-- Nullable legacy coordinates remain allowed as per SQL UNIQUE semantics.
ALTER TABLE tickets
    ADD CONSTRAINT uq_tickets_session_row_seat UNIQUE (session_id, fila, seat);
