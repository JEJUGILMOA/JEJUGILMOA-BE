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

-- 2a. place 데이터로 백필
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
  AND (trp.place_name IS NULL OR trp.visit_date IS NULL);

-- 2b. 백필 후에도 NULL이 남는 행을 dev용 기본값으로 채운다
UPDATE travel_record_place SET
    place_name  = COALESCE(place_name, '알 수 없음'),
    address     = COALESCE(address, '알 수 없음'),
    latitude    = COALESCE(latitude, 33.5),
    longitude   = COALESCE(longitude, 126.5),
    visit_date  = COALESCE(visit_date, CURRENT_DATE)
WHERE place_name IS NULL OR address IS NULL
   OR latitude IS NULL OR longitude IS NULL OR visit_date IS NULL;

-- 2c. NOT NULL 제약 적용 (이미 NOT NULL이면 건너뜀)
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'travel_record_place'
          AND column_name = 'place_name'
          AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE travel_record_place
            ALTER COLUMN place_name SET NOT NULL,
            ALTER COLUMN address    SET NOT NULL,
            ALTER COLUMN latitude   SET NOT NULL,
            ALTER COLUMN longitude  SET NOT NULL,
            ALTER COLUMN visit_date SET NOT NULL;
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
