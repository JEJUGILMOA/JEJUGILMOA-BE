ALTER TABLE recommended_course
    DROP COLUMN IF EXISTS rating,
    DROP COLUMN IF EXISTS is_free,
    DROP COLUMN IF EXISTS transport_mode;

ALTER TABLE recommended_course_path
    DROP COLUMN IF EXISTS recommended_duration;
