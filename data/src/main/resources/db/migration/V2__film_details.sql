ALTER TABLE films
    ADD COLUMN release_year INTEGER CHECK (release_year BETWEEN 1 AND 9999),
    ADD COLUMN director TEXT,
    ADD COLUMN actors TEXT[] NOT NULL DEFAULT '{}' CHECK (array_position(actors, NULL) IS NULL),
    ADD COLUMN duration_minutes INTEGER CHECK (duration_minutes > 0),
    ADD COLUMN genres TEXT[] NOT NULL DEFAULT '{}' CHECK (array_position(genres, NULL) IS NULL);
