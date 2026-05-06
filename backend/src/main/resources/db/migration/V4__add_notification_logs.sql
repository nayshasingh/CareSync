CREATE TABLE notification_logs (
    notification_log_id INT AUTO_INCREMENT,
    receiver_email VARCHAR(255),
    subject VARCHAR(255),
    status VARCHAR(20) CHECK (status IN ('SENT', 'FAILED')),
    error_message VARCHAR(500),
    created_at DATETIME(6),
    CONSTRAINT notification_log_pk PRIMARY KEY (notification_log_id)
);

CREATE INDEX idx_notification_logs_receiver_created
    ON notification_logs (receiver_email, created_at);

CREATE INDEX idx_notification_logs_status_created
    ON notification_logs (status, created_at);
