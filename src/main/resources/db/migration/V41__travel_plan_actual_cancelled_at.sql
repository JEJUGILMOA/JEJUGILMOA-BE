-- 여행 중단(POST /api/trips/{tripId}/cancel) API가 실제 중단 시각을 기록할 컬럼.
-- actual_started_at과 대응되며, IN_PROGRESS -> CANCELLED 전환이 실제 발생한 시각을 남긴다.
ALTER TABLE public.travel_plan ADD COLUMN actual_cancelled_at timestamp(6) without time zone;
