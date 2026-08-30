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
