-- Выполните один раз для нового пака. Повторный запуск создаст ещё один пак.
BEGIN;
WITH pack AS (
    INSERT INTO film_packs (title, description, premium)
    VALUES ('Вечер фантастики', 'Два фильма о космосе', false)
    RETURNING id
), first_film AS (
    INSERT INTO films (title, description, poster_url, release_year, director, actors, duration_minutes, genres)
    VALUES ('Интерстеллар', 'Экспедиция за пределы Солнечной системы',
            NULL, 2014, 'Кристофер Нолан', ARRAY['Мэттью Макконахи', 'Энн Хэтэуэй'], 169, ARRAY['Фантастика', 'Драма'])
    RETURNING id
), second_film AS (
    INSERT INTO films (title, description, poster_url, release_year, director, actors, duration_minutes, genres)
    VALUES ('Марсианин', 'Астронавт выживает на Марсе',
            NULL, 2015, 'Ридли Скотт', ARRAY['Мэтт Деймон', 'Джессика Честейн'], 144, ARRAY['Фантастика', 'Приключения'])
    RETURNING id
)
INSERT INTO pack_films (pack_id, film_id, position)
SELECT pack.id, first_film.id, 1 FROM pack, first_film
UNION ALL
SELECT pack.id, second_film.id, 2 FROM pack, second_film;
COMMIT;
