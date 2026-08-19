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

ALTER TABLE notification
    ADD CONSTRAINT uk_notification_id_user UNIQUE (id, user_id);

ALTER TABLE notification_read
    DROP CONSTRAINT fk_notification_read_notification,
    ADD CONSTRAINT fk_notification_read_notification_owner
        FOREIGN KEY (notification_id, user_id)
        REFERENCES notification (id, user_id);
