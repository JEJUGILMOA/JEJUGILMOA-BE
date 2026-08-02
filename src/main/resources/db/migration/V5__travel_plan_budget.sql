-- V5: travel_plan에 예산 필드 4개 추가 (STEP 07 예산 입력 기능)
ALTER TABLE public.travel_plan ADD COLUMN budget_transportation integer;
ALTER TABLE public.travel_plan ADD COLUMN budget_accommodation integer;
ALTER TABLE public.travel_plan ADD COLUMN budget_food integer;
ALTER TABLE public.travel_plan ADD COLUMN budget_etc integer;
