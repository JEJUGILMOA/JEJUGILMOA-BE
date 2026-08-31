-- Phase 1: 홈 화면 구현을 위한 스키마 확장

-- PopularPlace: 큐레이션 레이블 추가 (TODAY_PICK / TRAVELER_PICK)
ALTER TABLE popular_place
    ADD COLUMN curation_label VARCHAR(30);

CREATE INDEX idx_popular_place_curation_label ON popular_place(curation_label);

-- RecommendedCourse: 홈 화면 노출 필드 추가
ALTER TABLE recommended_course
    ADD COLUMN image_url         VARCHAR(500),
    ADD COLUMN tags              VARCHAR(200),
    ADD COLUMN estimated_minutes INTEGER,
    ADD COLUMN transport_mode    VARCHAR(20),
    ADD COLUMN rating            NUMERIC(3, 2),
    ADD COLUMN is_free           BOOLEAN;

-- RecommendedCoursePath: 경유지 간 이동 정보 추가
ALTER TABLE recommended_course_path
    ADD COLUMN travel_time_to_next  INTEGER,
    ADD COLUMN travel_mode_to_next  VARCHAR(20);
