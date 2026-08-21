-- travel_plan: destination 컬럼 제거 (CASCADE로 FK 제약 자동 삭제)
ALTER TABLE travel_plan
    DROP COLUMN IF EXISTS destination_place_id CASCADE,
    DROP COLUMN IF EXISTS destination_location_name;

-- travel_plan_preference: 기존 category_id → TravelTheme enum 백필
-- ELSE 없음: 매핑되지 않은 category.name은 theme을 NULL로 유지해 아래 검증에서 감지
UPDATE travel_plan_preference tp
SET theme = CASE c.name
    WHEN '음식' THEN 'FOOD'
    WHEN '자연' THEN 'NATURE'
    WHEN '체험' THEN 'ACTIVITY'
    WHEN '카페' THEN 'CAFE'
    WHEN '역사' THEN 'CULTURE'
    WHEN '쇼핑' THEN 'SHOPPING'
    WHEN '축제' THEN 'FESTIVAL'
END
FROM category c
WHERE tp.category_id = c.id
  AND tp.theme IS NULL;

-- 백필 후 theme이 NULL인 행이 있으면 마이그레이션 중단
-- (매핑되지 않은 category.name, 연결되지 않은 category_id, NULL category_id 모두 해당)
DO $$
DECLARE v INTEGER;
BEGIN
    SELECT COUNT(*) INTO v FROM travel_plan_preference WHERE theme IS NULL;
    IF v > 0 THEN
        RAISE EXCEPTION
            'V23: % 건의 travel_plan_preference 행이 알 수 없는 category_id를 가지고 있어 TravelTheme으로 변환할 수 없습니다. 수동으로 데이터를 확인하고 수정한 후 마이그레이션을 재실행하세요.',
            v;
    END IF;
END $$;

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
