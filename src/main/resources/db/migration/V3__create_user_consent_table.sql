CREATE TABLE user_consent (
    id              UUID PRIMARY KEY,
    user_id         VARCHAR(255) NOT NULL,
    document_type   VARCHAR(40) NOT NULL,
    version         VARCHAR(20) NOT NULL,
    accepted_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    ip_address      VARCHAR(64),
    user_agent      VARCHAR(500)
);

CREATE INDEX ix_user_consent_user_doc
    ON user_consent (user_id, document_type, accepted_at DESC);
