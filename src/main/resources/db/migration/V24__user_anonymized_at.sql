-- user: 탈퇴 30일 경과 후 개인정보 익명화 시각 기록

ALTER TABLE public."user" ADD COLUMN anonymized_at timestamp(6) without time zone;
