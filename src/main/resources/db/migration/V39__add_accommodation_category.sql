INSERT INTO category (name, description, created_at, updated_at)
VALUES ('숙박', '호텔, 펜션, 콘도, 캠핑 등 숙박시설', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;
