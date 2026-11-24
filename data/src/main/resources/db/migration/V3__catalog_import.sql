ALTER TABLE films ADD COLUMN poiskkino_id BIGINT;
CREATE INDEX films_poiskkino_idx ON films (poiskkino_id);
ALTER TABLE film_packs ADD COLUMN is_published BOOLEAN NOT NULL DEFAULT false;

CREATE VIEW incomplete_pack_films AS
SELECT pf.pack_id, f.id AS film_id, f.title
FROM pack_films pf JOIN films f ON f.id = pf.film_id
WHERE nullif(trim(f.title), '') IS NULL OR f.release_year IS NULL
   OR nullif(trim(f.description), '') IS NULL OR nullif(trim(f.poster_url), '') IS NULL;

CREATE VIEW visible_film_packs AS
SELECT p.* FROM film_packs p WHERE p.is_published
AND EXISTS (SELECT 1 FROM pack_films pf WHERE pf.pack_id = p.id)
AND NOT EXISTS (SELECT 1 FROM incomplete_pack_films i WHERE i.pack_id = p.id);

-- Preserve visibility for existing complete packs only.
UPDATE film_packs p SET is_published = true
WHERE EXISTS (SELECT 1 FROM pack_films pf WHERE pf.pack_id = p.id)
AND NOT EXISTS (SELECT 1 FROM incomplete_pack_films i WHERE i.pack_id = p.id);

CREATE TABLE poiskkino_requests (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX poiskkino_requests_time ON poiskkino_requests(requested_at);
CREATE TABLE poiskkino_quota (
    singleton BOOLEAN PRIMARY KEY DEFAULT true CHECK (singleton),
    blocked_until TIMESTAMPTZ
);
INSERT INTO poiskkino_quota DEFAULT VALUES;
