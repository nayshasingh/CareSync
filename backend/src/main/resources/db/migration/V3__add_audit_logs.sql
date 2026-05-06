CREATE TABLE audit_logs (
    audit_log_id INT AUTO_INCREMENT,
    actor_user_id INT,
    action VARCHAR(100),
    entity_type VARCHAR(100),
    entity_id INT,
    description VARCHAR(500),
    created_at DATETIME(6),
    CONSTRAINT audit_log_pk PRIMARY KEY (audit_log_id),
    CONSTRAINT audit_log_actor_fk FOREIGN KEY (actor_user_id) REFERENCES users (user_id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_logs_actor_created
    ON audit_logs (actor_user_id, created_at);

CREATE INDEX idx_audit_logs_entity_created
    ON audit_logs (entity_type, entity_id, created_at);
