-- V4에서 uk_course_plan_sequence → uk_course_plan_date_sequence 교체가 의도대로
-- 실서버에 반영되지 않은 경우를 대비한 보정 마이그레이션.
-- IF EXISTS / 조건 검사로 이미 올바른 상태인 DB에서도 안전하게 실행된다.

-- 1. 구 constraint 제거 (남아있는 경우에만)
ALTER TABLE travel_course DROP CONSTRAINT IF EXISTS uk_course_plan_sequence;

-- 2. 신 constraint 추가 (없는 경우에만)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_course_plan_date_sequence'
          AND conrelid = 'travel_course'::regclass
    ) THEN
        ALTER TABLE travel_course
            ADD CONSTRAINT uk_course_plan_date_sequence
            UNIQUE (plan_id, visit_date, sequence_order);
    END IF;
END $$;
