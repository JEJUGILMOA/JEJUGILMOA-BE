-- Report: reason → reason_summary(enum 기반), reason_detail 추가
ALTER TABLE report RENAME COLUMN reason TO reason_summary;
ALTER TABLE report ALTER COLUMN reason_summary TYPE VARCHAR(50);
ALTER TABLE report ADD COLUMN reason_detail TEXT;

-- 동일 유저가 같은 게시글에 중복 신고 방지
ALTER TABLE report
    ADD CONSTRAINT uq_report_reporter_target
    UNIQUE (reporter_id, target_type, target_id);

-- TravelRecord: 관리자 심사 상태 추가
ALTER TABLE travel_record
    ADD COLUMN admin_status VARCHAR(20) NOT NULL DEFAULT 'NORMAL';
