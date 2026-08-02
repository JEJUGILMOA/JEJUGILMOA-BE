-- 여행 시작(POST /api/trips) API가 실제 시작 시각을 기록할 컬럼.
-- 계획 단계의 start_date(예정일)와 별개로, DRAFT -> IN_PROGRESS 전환이 실제 발생한 시각을 남긴다.
ALTER TABLE public.travel_plan ADD COLUMN actual_started_at timestamp(6) without time zone;
