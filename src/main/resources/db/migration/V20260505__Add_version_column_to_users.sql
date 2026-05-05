-- Issue #181: users 테이블에 version 컬럼 추가 (JPA Optimistic Locking)
ALTER TABLE users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
