ALTER TABLE film_packs ADD COLUMN cover_file TEXT NOT NULL DEFAULT 'default.png'
    CHECK (length(cover_file) <= 160 AND cover_file ~ '^[A-Za-z0-9][A-Za-z0-9_-]*\.(png|jpg|jpeg|webp)$');

UPDATE films SET actors=actors[1:5] WHERE cardinality(actors)>5;

-- One decision applies across all memberships, including separately imported copies
-- carrying the same provider ID. Manually created films use their local ID.
CREATE FUNCTION film_is_decided(viewer BIGINT, candidate films) RETURNS BOOLEAN
LANGUAGE SQL STABLE AS $$
    SELECT EXISTS (
        SELECT 1 FROM film_library l JOIN films chosen ON chosen.id=l.film_id
        WHERE l.account_id=viewer AND
          (chosen.id=candidate.id OR chosen.poiskkino_id=candidate.poiskkino_id)
    )
$$;
