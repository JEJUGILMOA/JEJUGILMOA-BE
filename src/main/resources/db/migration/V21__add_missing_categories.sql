INSERT INTO category (name, description, created_at, updated_at)
VALUES
    ('체험', '체험 및 액티비티', NOW(), NOW()),
    ('역사', '역사 및 문화', NOW(), NOW()),
    ('쇼핑', '쇼핑 및 시장', NOW(), NOW()),
    ('축제', '축제 및 이벤트', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;
