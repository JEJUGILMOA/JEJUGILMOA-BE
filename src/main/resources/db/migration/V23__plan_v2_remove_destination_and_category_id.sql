-- travel_plan: destination 컬럼 제거 (CASCADE로 FK 제약 자동 삭제)
ALTER TABLE travel_plan
    DROP COLUMN IF EXISTS destination_place_id CASCADE,
    DROP COLUMN IF EXISTS destination_location_name;

-- travel_plan_preference: category_id 제거, theme NOT NULL 설정
UPDATE travel_plan_preference SET theme = 'NATURE' WHERE theme IS NULL;

ALTER TABLE travel_plan_preference
    DROP COLUMN IF EXISTS category_id CASCADE;

ALTER TABLE travel_plan_preference
    ALTER COLUMN theme SET NOT NULL;

-- theme 중복 행 제거 (V18 DEFAULT 'NATURE' 적용으로 생긴 중복 가능성 처리)
DELETE FROM travel_plan_preference a
USING travel_plan_preference b
WHERE a.id > b.id
  AND a.plan_id = b.plan_id
  AND a.theme = b.theme;

CREATE UNIQUE INDEX IF NOT EXISTS uk_plan_preference_theme ON travel_plan_preference(plan_id, theme);
