-- DayDeparture: day별 출발지 테이블 생성
CREATE TABLE IF NOT EXISTS day_departure (
    id              BIGSERIAL PRIMARY KEY,
    plan_id         BIGINT       NOT NULL REFERENCES travel_plan(id) ON DELETE CASCADE,
    visit_date      DATE         NOT NULL,
    place_id        BIGINT       REFERENCES place(id),
    location_name   VARCHAR(200),
    latitude        NUMERIC(10, 8),
    longitude       NUMERIC(11, 8),
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_day_departure_plan_date UNIQUE (plan_id, visit_date)
);

CREATE INDEX IF NOT EXISTS idx_day_departure_plan ON day_departure (plan_id);

-- 기존 travel_plan의 출발지 데이터를 start_date 기준으로 이관
INSERT INTO day_departure (plan_id, visit_date, place_id, location_name, latitude, longitude, created_at, updated_at)
SELECT id, start_date, departure_place_id, departure_location_name, departure_latitude, departure_longitude, now(), now()
FROM travel_plan
WHERE departure_location_name IS NOT NULL
   OR departure_place_id IS NOT NULL
   OR departure_latitude IS NOT NULL
ON CONFLICT (plan_id, visit_date) DO NOTHING;
