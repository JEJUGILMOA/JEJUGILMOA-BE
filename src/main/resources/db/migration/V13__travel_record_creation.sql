ALTER TABLE travel_record
    ADD CONSTRAINT uk_travel_record_plan UNIQUE (plan_id);

ALTER TABLE travel_record
    DROP CONSTRAINT fkovpew2i7roye3t29g56247pim,
    ADD CONSTRAINT fk_travel_record_plan
        FOREIGN KEY (plan_id) REFERENCES travel_plan(id) ON DELETE SET NULL;

ALTER TABLE travel_record_place
    ADD COLUMN place_name varchar(200),
    ADD COLUMN address varchar(500),
    ADD COLUMN latitude numeric(10,8),
    ADD COLUMN longitude numeric(11,8),
    ADD COLUMN visit_date date,
    ALTER COLUMN stay_minutes DROP NOT NULL,
    ALTER COLUMN rating DROP NOT NULL;

UPDATE travel_record_place trp
SET place_name = p.name,
    address = p.address,
    latitude = p.latitude,
    longitude = p.longitude,
    visit_date = COALESCE(tr.actual_start_date, tr.actual_end_date, tr.created_at::date)
FROM place p, travel_record tr
WHERE trp.travel_place_id = p.id
  AND trp.travel_record_id = tr.id;

ALTER TABLE travel_record_place
    ALTER COLUMN place_name SET NOT NULL,
    ALTER COLUMN address SET NOT NULL,
    ALTER COLUMN latitude SET NOT NULL,
    ALTER COLUMN longitude SET NOT NULL,
    ALTER COLUMN visit_date SET NOT NULL;

ALTER TABLE travel_record_image
    RENAME COLUMN image_url TO object_key;

ALTER TABLE travel_record_image
    ADD CONSTRAINT uk_record_image_place UNIQUE (travel_record_place_id);
