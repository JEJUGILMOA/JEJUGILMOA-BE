-- badge.display_group 추가: nullable로 추가 -> 기존 행 백필 -> NOT NULL 전환.
-- 백필값은 EXPLORATION(탐험)을 기본값으로 사용한다. badgeType만으로는 displayGroup을 안전하게
-- 유추할 수 없으므로(예: CATEGORY 배지가 GOURMET일 수도, EXPLORATION일 수도 있음),
-- 운영 반영 전 실제 배지 데이터에 맞게 값을 재검토해야 한다.
ALTER TABLE public.badge ADD COLUMN display_group character varying(30);

UPDATE public.badge SET display_group = 'EXPLORATION' WHERE display_group IS NULL;

ALTER TABLE public.badge ALTER COLUMN display_group SET NOT NULL;

ALTER TABLE public.badge ADD CONSTRAINT badge_display_group_check
    CHECK (((display_group)::text = ANY ((ARRAY['EXPLORATION'::character varying, 'GOURMET'::character varying, 'SOCIAL'::character varying])::text[])));

-- BadgeConditionRepository.findAllByBadgeIdIn 조회 패턴에 맞는 인덱스.
CREATE INDEX idx_badge_condition_badge ON public.badge_condition USING btree (badge_id);
