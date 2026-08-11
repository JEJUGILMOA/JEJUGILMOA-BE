-- 여행 완료(POST /api/trips/{tripId}/complete) API가 실제 완료 시각을 기록할 컬럼.
-- actual_started_at과 대응되며, IN_PROGRESS -> COMPLETED 전환이 실제 발생한 시각을 남긴다.
ALTER TABLE public.travel_plan ADD COLUMN actual_completed_at timestamp(6) without time zone;
