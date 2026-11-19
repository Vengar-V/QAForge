-- Міграція: конвертуємо PostgreSQL custom enum типи у VARCHAR
-- щоб Hibernate міг зберігати значення без конфліктів типів

ALTER TABLE test_scenarios
    ALTER COLUMN test_type  TYPE VARCHAR(50) USING test_type::VARCHAR,
    ALTER COLUMN status     TYPE VARCHAR(50) USING status::VARCHAR,
    ALTER COLUMN source     TYPE VARCHAR(50) USING source::VARCHAR;

-- Оновлюємо DEFAULT значення
ALTER TABLE test_scenarios
    ALTER COLUMN test_type  SET DEFAULT 'UNIT',
    ALTER COLUMN status     SET DEFAULT 'DRAFT',
    ALTER COLUMN source     SET DEFAULT 'AI_GENERATED';
