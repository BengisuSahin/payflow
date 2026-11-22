-- The publisher polls every second for the oldest unpublished events.
-- A partial index holds only unpublished rows, so it stays small as the
-- table grows and the poll never scans published history.
CREATE INDEX idx_outbox_events_unpublished
    ON outbox_events (created_at)
    WHERE published_at IS NULL;
