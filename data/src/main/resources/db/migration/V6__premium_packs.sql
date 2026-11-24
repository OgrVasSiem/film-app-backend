-- Reuse the same film records across packs. Fill missing titles on fresh installs.
DO $$
DECLARE
    collection RECORD;
    entry RECORD;
    pack_key BIGINT;
    film_key BIGINT;
BEGIN
    FOR collection IN SELECT * FROM (VALUES
        ('Игры разума','Истории, в которых герои пытаются разобраться в ловушках времени и сознания.',
         'mind.jpg', ARRAY['Начало','Исходный код','День сурка','Назад в будущее'],ARRAY[2010,2011,1993,1985]),
        ('На пределе','Четыре разных испытания на выносливость, смелость и самообладание.',
         'limits.jpg', ARRAY['Гравитация','Изгой','Эверест','Пленницы'],ARRAY[2013,2000,2015,2013])
    ) AS t(title,description,cover,titles,years)
    LOOP
        INSERT INTO film_packs(title,description,cover_file,premium)
        VALUES(collection.title,collection.description,collection.cover,true) RETURNING id INTO pack_key;
        FOR entry IN SELECT * FROM unnest(collection.titles,collection.years) WITH ORDINALITY AS x(title,yr,pos)
        LOOP
            SELECT id INTO film_key FROM films WHERE title=entry.title AND release_year=entry.yr
                ORDER BY (poiskkino_id IS NOT NULL) DESC,id LIMIT 1;
            IF film_key IS NULL THEN
                INSERT INTO films(title,release_year) VALUES(entry.title,entry.yr) RETURNING id INTO film_key;
            END IF;
            INSERT INTO pack_films VALUES(pack_key,film_key,entry.pos);
        END LOOP;
        UPDATE film_packs SET is_published=true WHERE id=pack_key
            AND NOT EXISTS(SELECT 1 FROM incomplete_pack_films WHERE pack_id=pack_key);
    END LOOP;
END $$;
