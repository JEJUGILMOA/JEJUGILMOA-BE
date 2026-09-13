-- V34 이후에도 legacy 컬럼이 남아 있는 DB를 보정한다.
-- 정상적으로 제거된 DB에서도 적용할 수 있도록 IF EXISTS를 사용한다.
ALTER TABLE travel_record_image
    DROP COLUMN IF EXISTS image_url;
