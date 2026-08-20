-- old V13이 RENAME/ADD COLUMN 없이 적용된 DB를 보정한다.
-- validate-on-migrate: false 환경에서 V13 체크섬 불일치를 우회한 결과로
-- 일부 스키마 변경이 누락됐을 수 있다. 각 구문은 멱등(idempotent)하게 작성했다.

-- 1. travel_record_image: image_url → object_key
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'travel_record_image'
          AND column_name = 'image_url'
    ) THEN
        ALTER TABLE travel_record_image RENAME COLUMN image_url TO object_key;
    END IF;
END $$;

-- 2. travel_record_place 신규 컬럼 추가
ALTER TABLE travel_record_place
    ADD COLUMN IF NOT EXISTS place_name varchar(200),
    ADD COLUMN IF NOT EXISTS address    varchar(500),
    ADD COLUMN IF NOT EXISTS latitude   numeric(10,8),
    ADD COLUMN IF NOT EXISTS longitude  numeric(11,8),
    ADD COLUMN IF NOT EXISTS visit_date date;

-- 2a. place 데이터로 백필 (모든 대상 컬럼의 NULL을 조건에 포함)
UPDATE travel_record_place trp
SET place_name = COALESCE(trp.place_name, p.name),
    address    = COALESCE(trp.address, p.address),
    latitude   = COALESCE(trp.latitude, p.latitude),
    longitude  = COALESCE(trp.longitude, p.longitude),
    visit_date = COALESCE(trp.visit_date,
                          tr.actual_start_date, tr.actual_end_date, tr.created_at::date)
FROM place p, travel_record tr
WHERE trp.travel_place_id = p.id
  AND trp.travel_record_id = tr.id
  AND (trp.place_name IS NULL OR trp.address IS NULL
       OR trp.latitude IS NULL OR trp.longitude IS NULL OR trp.visit_date IS NULL);

-- 2b. 백필 후에도 NULL이 남는 행은 원본 데이터가 없는 것이므로 삭제 (dev 전용 정제)
DELETE FROM travel_record_place
WHERE place_name IS NULL OR address IS NULL
   OR latitude IS NULL OR longitude IS NULL OR visit_date IS NULL;

-- 2c. NOT NULL 제약 적용 (컬럼별로 개별 검사)
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'travel_record_place'
          AND column_name = 'place_name' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE travel_record_place ALTER COLUMN place_name SET NOT NULL;
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'travel_record_place'
          AND column_name = 'address' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE travel_record_place ALTER COLUMN address SET NOT NULL;
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'travel_record_place'
          AND column_name = 'latitude' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE travel_record_place ALTER COLUMN latitude SET NOT NULL;
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'travel_record_place'
          AND column_name = 'longitude' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE travel_record_place ALTER COLUMN longitude SET NOT NULL;
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'travel_record_place'
          AND column_name = 'visit_date' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE travel_record_place ALTER COLUMN visit_date SET NOT NULL;
    END IF;
END $$;

-- 3. stay_minutes / rating nullable 전환 (이미 nullable이면 no-op)
ALTER TABLE travel_record_place
    ALTER COLUMN stay_minutes DROP NOT NULL,
    ALTER COLUMN rating       DROP NOT NULL;

-- 4. uk_record_image_place 제약 추가 (없는 경우만)
DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_record_image_place'
    ) THEN
        ALTER TABLE travel_record_image
            ADD CONSTRAINT uk_record_image_place UNIQUE (travel_record_place_id);
    END IF;
END $$;

-- 5. V14 보정: 제약·인덱스·컬럼 멱등 처리
-- V14 원본을 복원했으므로, V14가 부분 실패한 드리프트 DB를 위한 안전망

-- uk_travel_record_plan 제약이 아직 남아있으면 제거
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_travel_record_plan'
    ) THEN
        ALTER TABLE travel_record DROP CONSTRAINT uk_travel_record_plan;
    END IF;
END $$;

-- title: varchar(50) 전환 (이미 적용된 경우 no-op)
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'travel_record'
          AND column_name = 'title'
          AND NOT (data_type = 'character varying' AND character_maximum_length = 50)
    ) THEN
        ALTER TABLE travel_record ALTER COLUMN title TYPE varchar(50);
    END IF;
END $$;

-- title: NOT NULL 전환 (이미 적용된 경우 no-op)
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'travel_record'
          AND column_name = 'title'
          AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE travel_record ALTER COLUMN title SET NOT NULL;
    END IF;
END $$;

-- uk_travel_record_active_plan 인덱스 생성 (이미 존재하면 no-op)
CREATE UNIQUE INDEX IF NOT EXISTS uk_travel_record_active_plan
    ON travel_record (plan_id)
    WHERE deleted_at IS NULL;
