-- 경유지 건너뛰기: 실제 GPS 방문 인증과 건너뛰기를 구분해, 뱃지 집계에서 건너뛴 경유지를 제외하기 위한 컬럼.
ALTER TABLE public.travel_course
    ADD COLUMN is_skipped boolean NOT NULL DEFAULT false,
    ADD COLUMN skipped_at timestamp(6) without time zone;
