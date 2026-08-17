ALTER TABLE travel_record
    DROP CONSTRAINT uk_travel_record_plan,
    ALTER COLUMN title TYPE varchar(50),
    ALTER COLUMN title SET NOT NULL;

ALTER TABLE travel_record_place
    ALTER COLUMN memo TYPE varchar(1000);

CREATE UNIQUE INDEX uk_travel_record_active_plan
    ON travel_record (plan_id)
    WHERE deleted_at IS NULL;
