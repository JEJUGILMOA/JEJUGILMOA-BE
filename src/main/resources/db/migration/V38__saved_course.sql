CREATE TABLE saved_course (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT    NOT NULL REFERENCES "user"(id),
    source_type           VARCHAR(20) NOT NULL,
    recommended_course_id BIGINT    REFERENCES recommended_course(id),
    travel_record_id      BIGINT    REFERENCES travel_record(id),
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_saved_course_user ON saved_course(user_id);

-- 동일 유저가 같은 추천 코스를 중복 담기 방지 (PostgreSQL partial index)
CREATE UNIQUE INDEX uq_saved_course_recommended
    ON saved_course(user_id, recommended_course_id)
    WHERE recommended_course_id IS NOT NULL;

-- 동일 유저가 같은 기록 코스를 중복 담기 방지 (PostgreSQL partial index)
CREATE UNIQUE INDEX uq_saved_course_record
    ON saved_course(user_id, travel_record_id)
    WHERE travel_record_id IS NOT NULL;
