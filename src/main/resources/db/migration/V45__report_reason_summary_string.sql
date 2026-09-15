-- reason_summary: enum 기반 VARCHAR(50) → 사용자 직접 입력 VARCHAR(200)
ALTER TABLE report ALTER COLUMN reason_summary TYPE VARCHAR(200);
