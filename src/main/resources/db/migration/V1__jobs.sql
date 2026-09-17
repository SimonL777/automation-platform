CREATE TABLE jobs (
 id VARCHAR(36) PRIMARY KEY,
 owner_id VARCHAR(128) NOT NULL,
 idempotency_key VARCHAR(128) NOT NULL,
 request_hash VARCHAR(64) NOT NULL,
 payload TEXT NOT NULL,
 status VARCHAR(24) NOT NULL,
 lease_id VARCHAR(36),
 lease_until BIGINT NOT NULL DEFAULT 0,
 remote_id VARCHAR(128),
 cleanup_status VARCHAR(24) NOT NULL DEFAULT 'NOT_REQUIRED',
 result TEXT,
 error_code VARCHAR(80),
 created_at BIGINT NOT NULL,
 updated_at BIGINT NOT NULL,
 CONSTRAINT jobs_idempotency UNIQUE(owner_id,idempotency_key)
);
CREATE INDEX jobs_claim ON jobs(status,created_at);
CREATE INDEX jobs_owner ON jobs(owner_id,created_at);
CREATE INDEX jobs_lease ON jobs(status,lease_until);
