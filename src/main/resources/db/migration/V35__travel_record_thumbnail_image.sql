ALTER TABLE travel_record
    ADD COLUMN thumbnail_image_id bigint;

ALTER TABLE travel_record
    ADD CONSTRAINT fk_travel_record_thumbnail_image
        FOREIGN KEY (thumbnail_image_id)
        REFERENCES travel_record_image(id)
        ON DELETE SET NULL;

CREATE INDEX idx_record_thumbnail_image
    ON travel_record (thumbnail_image_id);

UPDATE travel_record record
SET thumbnail_image_id = (
    SELECT image.id
    FROM travel_record_image image
    WHERE image.travel_record_id = record.id
    ORDER BY image.sequence_order ASC
    LIMIT 1
);
