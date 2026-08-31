-- Place 테이블에서 제거된 필드 정리.
-- 이전 마이그레이션에서 추가됐다가 엔티티에서 제거된 컬럼들.
-- IF EXISTS로 이미 없는 컬럼도 안전하게 처리.
ALTER TABLE place
    DROP COLUMN IF EXISTS homepage,
    DROP COLUMN IF EXISTS tel,
    DROP COLUMN IF EXISTS opening_hours,
    DROP COLUMN IF EXISTS admission_fee,
    DROP COLUMN IF EXISTS atmosphere_tag,
    DROP COLUMN IF EXISTS hashtags;
