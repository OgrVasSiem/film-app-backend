CREATE TABLE accounts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    login TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    premium BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE sessions (
    token_hash TEXT PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX sessions_expiry_idx ON sessions (expires_at);

CREATE TABLE film_packs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    premium BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE films (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    poster_url TEXT
);

CREATE TABLE pack_films (
    pack_id BIGINT NOT NULL REFERENCES film_packs(id) ON DELETE CASCADE,
    film_id BIGINT NOT NULL REFERENCES films(id) ON DELETE CASCADE,
    position INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (pack_id, film_id)
);
CREATE INDEX pack_films_film_idx ON pack_films (film_id);

CREATE TABLE film_library (
    account_id BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    film_id BIGINT NOT NULL REFERENCES films(id) ON DELETE CASCADE,
    status TEXT NOT NULL CHECK (status IN ('WILL_WATCH', 'WONT_WATCH', 'WATCHED')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (account_id, film_id)
);
CREATE INDEX film_library_status_idx ON film_library (account_id, status, updated_at DESC);
