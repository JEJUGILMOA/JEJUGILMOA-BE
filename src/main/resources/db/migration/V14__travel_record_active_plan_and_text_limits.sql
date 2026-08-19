ALTER TABLE travel_record
    DROP CONSTRAINT IF EXISTS uk_travel_record_plan,
    ADD CONSTRAINT travel_record_title_required_and_length_check
        CHECK (title IS NOT NULL AND char_length(title) <= 50) NOT VALID;

-- NOT VALID는 기존 50자 초과 제목을 그대로 보존하면서도 이후 INSERT/UPDATE에는
-- 제목 필수 및 50자 제한을 즉시 적용한다. 기존 데이터를 정리한 뒤 별도 migration에서
-- VALIDATE CONSTRAINT와 varchar(50) 전환을 수행한다.

ALTER TABLE travel_record_place
    ALTER COLUMN memo TYPE varchar(1000);

CREATE UNIQUE INDEX IF NOT EXISTS uk_travel_record_active_plan
    ON travel_record (plan_id)
    WHERE deleted_at IS NULL;
