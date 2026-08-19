-- token 컬럼: 일반 인덱스를 제거하고 유니크 제약으로 교체
DROP INDEX IF EXISTS idx_device_token_token;
ALTER TABLE public.device_token ADD CONSTRAINT uk_device_token_token UNIQUE (token);
