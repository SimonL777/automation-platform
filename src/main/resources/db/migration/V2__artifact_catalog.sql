CREATE TABLE artifact_catalog (
 task_id VARCHAR(36) NOT NULL, owner_id VARCHAR(128) NOT NULL, name VARCHAR(120) NOT NULL,
 size_bytes BIGINT NOT NULL, sha256 VARCHAR(64) NOT NULL, backend VARCHAR(24) NOT NULL,
 created_at BIGINT NOT NULL, PRIMARY KEY(task_id,name)
);
CREATE INDEX artifact_owner_created ON artifact_catalog(owner_id,created_at);
