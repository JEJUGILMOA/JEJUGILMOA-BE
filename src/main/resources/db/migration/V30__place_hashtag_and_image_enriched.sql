CREATE TABLE place_hashtag (
    id          BIGSERIAL   PRIMARY KEY,
    place_id    BIGINT      NOT NULL UNIQUE REFERENCES place(id),
    mid_label   VARCHAR(100),
    sub_label   VARCHAR(100)
);

CREATE INDEX idx_place_hashtag_place ON place_hashtag(place_id);

ALTER TABLE place ADD COLUMN image_enriched BOOLEAN NOT NULL DEFAULT FALSE;
