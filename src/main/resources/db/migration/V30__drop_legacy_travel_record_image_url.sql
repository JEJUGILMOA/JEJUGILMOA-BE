-- TravelRecordImage의 저장 필드는 object_key이며 image_url은 과거 엔티티의 legacy 컬럼이다.
-- V13/V19 적용 이후 ddl-auto:update 등으로 image_url이 다시 생성된 드리프트 DB를 정리한다.
ALTER TABLE travel_record_image
    DROP COLUMN IF EXISTS image_url;
