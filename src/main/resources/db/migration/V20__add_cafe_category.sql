INSERT INTO category (name, description, created_at, updated_at)
VALUES ('카페', '카페 및 디저트', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;
