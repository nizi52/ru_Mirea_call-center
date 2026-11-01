-- Перевод уже существующей базы на новую схему: departments → call_centers и колонка clients.is_active.
-- Нужен, только если база создавалась старым init.sql: Docker выполняет init.sql лишь при первом
-- создании volume, поэтому сам он таблицы не переименует. Данные (обращения, клиенты, операторы) сохраняются.
-- Скрипт можно запускать повторно: если переходить уже не нужно, он ничего не делает.
--
-- Запуск через Docker (PowerShell):
--   Get-Content database/migrate-to-call-centers.sql | docker exec -i callcenter-pg psql -U postgres -d call_center

DO $$
BEGIN
    IF to_regclass('public.departments') IS NOT NULL AND to_regclass('public.call_centers') IS NULL THEN
        ALTER TABLE departments RENAME TO call_centers;
        ALTER TABLE call_centers ADD COLUMN address VARCHAR(200);
        -- у отделов не было адреса: переносим описание, а пустые заполняем заглушкой
        UPDATE call_centers SET address = COALESCE(NULLIF(trim(description), ''), 'Адрес не указан');
        ALTER TABLE call_centers ALTER COLUMN address SET NOT NULL;
        ALTER TABLE call_centers DROP COLUMN description;
        ALTER TABLE operators RENAME COLUMN department_id TO call_center_id;
        RAISE NOTICE 'Готово: departments переименована в call_centers.';
    ELSE
        RAISE NOTICE 'Переход не нужен: таблица call_centers уже есть.';
    END IF;
END $$;

-- Активность клиента (добавлено позже). IF NOT EXISTS — повторный запуск ничего не сломает.
ALTER TABLE clients ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
