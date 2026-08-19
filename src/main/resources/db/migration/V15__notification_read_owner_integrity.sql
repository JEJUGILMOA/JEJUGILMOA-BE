DO $$
DECLARE
    invalid_pairs text;
BEGIN
    SELECT string_agg(
                   format('(notification_read.id=%s, notification_id=%s, read_user_id=%s, notification_user_id=%s)',
                          invalid.id, invalid.notification_id, invalid.read_user_id, invalid.notification_user_id),
                   ', '
           )
      INTO invalid_pairs
      FROM (
          SELECT nr.id,
                 nr.notification_id,
                 nr.user_id AS read_user_id,
                 n.user_id AS notification_user_id
            FROM notification_read nr
            JOIN notification n ON n.id = nr.notification_id
           WHERE nr.user_id <> n.user_id
           ORDER BY nr.id
           LIMIT 20
      ) invalid;

    IF invalid_pairs IS NOT NULL THEN
        RAISE EXCEPTION
            'notification_read owner mismatch blocks migration. Fix the listed rows without deleting notification history: %',
            invalid_pairs;
    END IF;
END $$;

-- application.yml의 spring.flyway.mixed=true 설정으로 트랜잭션 밖에서 실행된다.
-- notification 쓰기를 막는 일반 UNIQUE constraint용 인덱스 생성 대신 concurrent build를 사용한다.
CREATE UNIQUE INDEX CONCURRENTLY uk_notification_id_user_idx
    ON notification (id, user_id);

ALTER TABLE notification
    ADD CONSTRAINT uk_notification_id_user
        UNIQUE USING INDEX uk_notification_id_user_idx;

ALTER TABLE notification_read
    ADD CONSTRAINT fk_notification_read_notification_owner
        FOREIGN KEY (notification_id, user_id)
        REFERENCES notification (id, user_id)
        NOT VALID;

ALTER TABLE notification_read
    VALIDATE CONSTRAINT fk_notification_read_notification_owner;

-- 복합 FK가 기존 행까지 검증된 후에만 중복되는 단일 FK를 제거한다.
ALTER TABLE notification_read
    DROP CONSTRAINT fk_notification_read_notification;
