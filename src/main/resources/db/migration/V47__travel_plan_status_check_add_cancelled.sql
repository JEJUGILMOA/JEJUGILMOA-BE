-- TravelPlanStatus enum에는 CANCELLED가 있고 TravelPlan.cancel()도 이 값을 세팅하지만,
-- V1 baseline의 travel_plan_status_check는 DRAFT/IN_PROGRESS/COMPLETED만 허용해
-- 여행 중단(POST /api/trips/{tripId}/cancel) 호출 시 check constraint violation이 발생한다.
-- NOT VALID로 추가 후 VALIDATE: 기존 행은 이미 constraint를 만족하므로 즉시 유효화해도 안전하다.

ALTER TABLE public.travel_plan DROP CONSTRAINT IF EXISTS travel_plan_status_check;

ALTER TABLE public.travel_plan ADD CONSTRAINT travel_plan_status_check
    CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[]))) NOT VALID;

ALTER TABLE public.travel_plan VALIDATE CONSTRAINT travel_plan_status_check;
