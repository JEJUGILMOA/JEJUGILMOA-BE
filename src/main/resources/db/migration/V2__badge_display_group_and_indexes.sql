-- badge.display_group 추가: nullable로 추가 -> 기존 행 백필 -> NOT VALID 제약 추가/검증 -> NOT NULL 전환.
-- 백필값은 EXPLORATION(탐험)을 기본값으로 사용한다. badgeType만으로는 displayGroup을 안전하게
-- 유추할 수 없어(예: CATEGORY 배지가 GOURMET일 수도, EXPLORATION일 수도 있음) 원래는 위험한
-- 가정이었으나, 운영 DB(gilmoa_dev) badge 테이블을 직접 조회해 0건임을 확인했다(2026-07-31).
-- 즉 이 UPDATE는 현재 no-op이다. 이후 배지 시딩/생성 기능이 실제 배지를 넣기 시작하면
-- 이 기본값 로직은 더 이상 유효하지 않으므로 반드시 재검토해야 한다.
--
-- badge는 소규모 배지 카탈로그 테이블(대량 사용자 데이터 아님)이라 백필을 배치로 나누지 않았다.
-- 대용량 테이블에 동일 패턴을 적용할 때는 UPDATE를 청크로 나눠 커밋해야 한다.
ALTER TABLE public.badge ADD COLUMN display_group character varying(30);

UPDATE public.badge SET display_group = 'EXPLORATION' WHERE display_group IS NULL;

-- NOT VALID로 추가 후 VALIDATE: 이미 검증된 CHECK 제약이 있으면 SET NOT NULL이
-- 테이블 풀스캔 없이 즉시(메타데이터만 변경) 적용된다 (PostgreSQL 12+).
ALTER TABLE public.badge ADD CONSTRAINT badge_display_group_not_null_check
    CHECK (display_group IS NOT NULL) NOT VALID;
ALTER TABLE public.badge VALIDATE CONSTRAINT badge_display_group_not_null_check;
ALTER TABLE public.badge ALTER COLUMN display_group SET NOT NULL;
ALTER TABLE public.badge DROP CONSTRAINT badge_display_group_not_null_check;

ALTER TABLE public.badge ADD CONSTRAINT badge_display_group_check
    CHECK (((display_group)::text = ANY ((ARRAY['EXPLORATION'::character varying, 'GOURMET'::character varying, 'SOCIAL'::character varying])::text[]))) NOT VALID;
ALTER TABLE public.badge VALIDATE CONSTRAINT badge_display_group_check;
