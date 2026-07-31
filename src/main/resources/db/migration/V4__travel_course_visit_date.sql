-- V4: travel_course에 visit_date(Day 구분) 추가, memo 컬럼 제거, unique constraint 변경

-- 1. memo 컬럼 제거 (Java 엔티티에서 이미 제거됨, ddl-auto:update는 drop을 수행하지 않음)
ALTER TABLE public.travel_course DROP COLUMN IF EXISTS memo;

-- 2. visit_date 컬럼 추가 — 기존 rows는 소속 plan의 start_date로 채움
ALTER TABLE public.travel_course ADD COLUMN visit_date date;
UPDATE public.travel_course tc
    SET visit_date = (SELECT tp.start_date FROM public.travel_plan tp WHERE tp.id = tc.plan_id);
ALTER TABLE public.travel_course ALTER COLUMN visit_date SET NOT NULL;

-- 3. 기존 (plan_id, sequence_order) unique constraint 제거
ALTER TABLE public.travel_course DROP CONSTRAINT uk_course_plan_sequence;

-- 4. Day별 순서 보장: (plan_id, visit_date, sequence_order) unique constraint 추가
ALTER TABLE public.travel_course
    ADD CONSTRAINT uk_course_plan_date_sequence UNIQUE (plan_id, visit_date, sequence_order);

-- 5. Day별 경유지 조회 성능을 위한 복합 인덱스
CREATE INDEX idx_course_plan_date ON public.travel_course (plan_id, visit_date);
