-- TravelPlan: 동반자 유형, 출발지 좌표 추가
ALTER TABLE travel_plan
    ADD COLUMN IF NOT EXISTS companion          VARCHAR(20),
    ADD COLUMN IF NOT EXISTS departure_latitude  NUMERIC(10, 8),
    ADD COLUMN IF NOT EXISTS departure_longitude NUMERIC(11, 8);

-- TravelCourse: 선호 경유지 여부 추가
ALTER TABLE travel_course
    ADD COLUMN IF NOT EXISTS is_preferred BOOLEAN NOT NULL DEFAULT FALSE;

-- TravelPlanPreference: 테마 컬럼 추가 (category_id 는 다음 PR에서 제거)
ALTER TABLE travel_plan_preference
    ADD COLUMN IF NOT EXISTS theme VARCHAR(30);
