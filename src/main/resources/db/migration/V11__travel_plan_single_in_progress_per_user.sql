-- 유저당 진행중(IN_PROGRESS) 여행은 최대 1건이라는 비즈니스 규칙을 DB 레벨에서 강제한다.
-- 서로 다른 DRAFT 계획에 대한 동시 시작(POST /api/trips/start) 요청은 각기 다른 행을 잠그므로
-- 애플리케이션 레벨의 행 잠금만으로는 막을 수 없다 — 부분 유니크 인덱스로 두 번째 커밋을 막는다.
-- CONCURRENTLY는 트랜잭션 내에서 실행할 수 없어 별도 마이그레이션 파일로 분리했다 (V3와 동일한 이유).
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS idx_travel_plan_user_in_progress
    ON public.travel_plan (user_id)
    WHERE status = 'IN_PROGRESS';
