WITH pack AS (
    INSERT INTO film_packs (title, description) VALUES ('Время и память', 'Фильмы об устройстве памяти, времени и сновидений') RETURNING id
), entries(title, yr, pos) AS (
    VALUES ('Начало', 2010, 1), ('Помни', 2000, 2), ('Эффект бабочки', 2004, 3), ('Исходный код', 2011, 4)
), existing AS (
    SELECT DISTINCT ON (e.pos) e.pos, f.id FROM entries e JOIN films f
    ON f.title=e.title AND f.release_year=e.yr ORDER BY e.pos, f.id
), added AS (
    INSERT INTO films(title,release_year)
    SELECT title,yr FROM entries e WHERE NOT EXISTS (SELECT 1 FROM existing x WHERE x.pos=e.pos)
    RETURNING id,title,release_year
), cards AS (
    SELECT pos,id FROM existing UNION ALL
    SELECT e.pos,a.id FROM added a JOIN entries e ON a.title=e.title AND a.release_year=e.yr
)
INSERT INTO pack_films(pack_id,film_id,position)
SELECT pack.id,cards.id,cards.pos FROM pack CROSS JOIN cards;

WITH pack AS (
    INSERT INTO film_packs (title, description) VALUES ('Миры анимации', 'Четыре истории, рассказанные средствами анимации') RETURNING id
), entries(title, yr, pos) AS (
    VALUES ('Рататуй', 2007, 1), ('ВАЛЛ·И', 2008, 2), ('Головоломка', 2015, 3), ('Тайна Коко', 2017, 4)
), existing AS (
    SELECT DISTINCT ON (e.pos) e.pos, f.id FROM entries e JOIN films f
    ON f.title=e.title AND f.release_year=e.yr ORDER BY e.pos, f.id
), added AS (
    INSERT INTO films(title,release_year)
    SELECT title,yr FROM entries e WHERE NOT EXISTS (SELECT 1 FROM existing x WHERE x.pos=e.pos)
    RETURNING id,title,release_year
), cards AS (
    SELECT pos,id FROM existing UNION ALL
    SELECT e.pos,a.id FROM added a JOIN entries e ON a.title=e.title AND a.release_year=e.yr
)
INSERT INTO pack_films(pack_id,film_id,position)
SELECT pack.id,cards.id,cards.pos FROM pack CROSS JOIN cards;

WITH pack AS (
    INSERT INTO film_packs (title, description) VALUES ('Расследования', 'Детективные истории с разными подходами к расследованию') RETURNING id
), entries(title, yr, pos) AS (
    VALUES ('Зодиак', 2007, 1), ('Остров проклятых', 2010, 2), ('Достать ножи', 2019, 3), ('Пленницы', 2013, 4)
), existing AS (
    SELECT DISTINCT ON (e.pos) e.pos, f.id FROM entries e JOIN films f
    ON f.title=e.title AND f.release_year=e.yr ORDER BY e.pos, f.id
), added AS (
    INSERT INTO films(title,release_year)
    SELECT title,yr FROM entries e WHERE NOT EXISTS (SELECT 1 FROM existing x WHERE x.pos=e.pos)
    RETURNING id,title,release_year
), cards AS (
    SELECT pos,id FROM existing UNION ALL
    SELECT e.pos,a.id FROM added a JOIN entries e ON a.title=e.title AND a.release_year=e.yr
)
INSERT INTO pack_films(pack_id,film_id,position)
SELECT pack.id,cards.id,cards.pos FROM pack CROSS JOIN cards;

WITH pack AS (
    INSERT INTO film_packs (title, description) VALUES ('Один на один со стихией', 'Истории выживания вдали от дома') RETURNING id
), entries(title, yr, pos) AS (
    VALUES ('Изгой', 2000, 1), ('Гравитация', 2013, 2), ('127 часов', 2010, 3), ('Эверест', 2015, 4)
), existing AS (
    SELECT DISTINCT ON (e.pos) e.pos, f.id FROM entries e JOIN films f
    ON f.title=e.title AND f.release_year=e.yr ORDER BY e.pos, f.id
), added AS (
    INSERT INTO films(title,release_year)
    SELECT title,yr FROM entries e WHERE NOT EXISTS (SELECT 1 FROM existing x WHERE x.pos=e.pos)
    RETURNING id,title,release_year
), cards AS (
    SELECT pos,id FROM existing UNION ALL
    SELECT e.pos,a.id FROM added a JOIN entries e ON a.title=e.title AND a.release_year=e.yr
)
INSERT INTO pack_films(pack_id,film_id,position)
SELECT pack.id,cards.id,cards.pos FROM pack CROSS JOIN cards;