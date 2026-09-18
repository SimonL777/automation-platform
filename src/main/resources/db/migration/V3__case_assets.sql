CREATE TABLE case_assets (
 id VARCHAR(36) PRIMARY KEY,owner_id VARCHAR(128) NOT NULL,title VARCHAR(160) NOT NULL,
 channel VARCHAR(8) NOT NULL,description TEXT NOT NULL,workflow TEXT NOT NULL,
 revision INTEGER NOT NULL,status VARCHAR(20) NOT NULL,tags TEXT NOT NULL,demo BOOLEAN NOT NULL,
 created_at BIGINT NOT NULL,updated_at BIGINT NOT NULL
);
CREATE INDEX case_assets_owner ON case_assets(owner_id,updated_at);
CREATE TABLE case_revisions (
 case_id VARCHAR(36) NOT NULL,revision INTEGER NOT NULL,workflow TEXT NOT NULL,
 workflow_hash VARCHAR(64) NOT NULL,created_at BIGINT NOT NULL,PRIMARY KEY(case_id,revision)
);
