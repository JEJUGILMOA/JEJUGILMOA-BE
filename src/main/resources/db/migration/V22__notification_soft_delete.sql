-- notification: 스와이프 삭제(소프트 삭제) 지원

ALTER TABLE public.notification ADD COLUMN deleted_at timestamp(6) without time zone;
