-- ============================================================
-- QAForge — PostgreSQL Schema
-- Flyway migration: V1__init_schema.sql
-- Path: src/main/resources/db/migration/V1__init_schema.sql
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ENUMS
CREATE TYPE import_type AS ENUM ('GITHUB', 'ARCHIVE');
CREATE TYPE project_status AS ENUM ('PENDING', 'IMPORTING', 'READY', 'ERROR');
CREATE TYPE risk_level AS ENUM ('LOW', 'MEDIUM', 'HIGH');
CREATE TYPE test_type AS ENUM ('UNIT', 'INTEGRATION', 'EDGE_CASE', 'NEGATIVE', 'REGRESSION', 'SECURITY');
CREATE TYPE test_status AS ENUM ('DRAFT', 'PASS', 'FAIL', 'SKIPPED');
CREATE TYPE test_source AS ENUM ('AI_GENERATED', 'MANUAL');
CREATE TYPE bug_severity AS ENUM ('MINOR', 'MAJOR', 'CRITICAL');
CREATE TYPE bug_status AS ENUM ('OPEN', 'IN_PROGRESS', 'CLOSED');

-- USERS
CREATE TABLE users (
                       id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255),
                       full_name     VARCHAR(255),
                       github_id     VARCHAR(100),
                       github_token  VARCHAR(500),
                       created_at    TIMESTAMP NOT NULL DEFAULT now(),
                       updated_at    TIMESTAMP NOT NULL DEFAULT now()
);

-- PROJECTS
CREATE TABLE projects (
                          id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          name           VARCHAR(255) NOT NULL,
                          repo_url       VARCHAR(500),
                          language       VARCHAR(50),
                          branch         VARCHAR(100) DEFAULT 'main',
                          import_type    import_type NOT NULL DEFAULT 'GITHUB',
                          status         project_status NOT NULL DEFAULT 'PENDING',
                          last_synced_at TIMESTAMP,
                          created_at     TIMESTAMP NOT NULL DEFAULT now(),
                          updated_at     TIMESTAMP NOT NULL DEFAULT now()
);

-- COMMIT HISTORY
CREATE TABLE commit_history (
                                id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                project_id    UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                                commit_hash   VARCHAR(40) NOT NULL,
                                message       TEXT,
                                author        VARCHAR(255),
                                is_bug_fix    BOOLEAN NOT NULL DEFAULT false,
                                files_changed JSONB,
                                committed_at  TIMESTAMP NOT NULL,
                                UNIQUE(project_id, commit_hash)
);

-- FILE RISKS
CREATE TABLE file_risks (
                            id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                            project_id     UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                            file_path      VARCHAR(500) NOT NULL,
                            risk_score     DECIMAL(4,2) NOT NULL DEFAULT 0,
                            bug_count      INT NOT NULL DEFAULT 0,
                            commit_count   INT NOT NULL DEFAULT 0,
                            coverage_pct   DECIMAL(5,2),
                            change_count   INT NOT NULL DEFAULT 0,
                            risk_level     risk_level NOT NULL DEFAULT 'LOW',
                            calculated_at  TIMESTAMP NOT NULL DEFAULT now(),
                            UNIQUE(project_id, file_path)
);

-- TEST SCENARIOS
CREATE TABLE test_scenarios (
                                id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                                file_risk_id    UUID REFERENCES file_risks(id) ON DELETE SET NULL,
                                title           VARCHAR(500) NOT NULL,
                                description     TEXT,
                                steps           JSONB NOT NULL DEFAULT '[]',
                                expected_result TEXT,
                                test_type       test_type NOT NULL DEFAULT 'UNIT',
                                status          test_status NOT NULL DEFAULT 'DRAFT',
                                source          test_source NOT NULL DEFAULT 'MANUAL',
                                tags            JSONB DEFAULT '[]',
                                created_at      TIMESTAMP NOT NULL DEFAULT now(),
                                updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- BUG REPORTS
CREATE TABLE bug_reports (
                             id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             project_id          UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                             reporter_id         UUID NOT NULL REFERENCES users(id),
                             title               VARCHAR(500) NOT NULL,
                             steps_to_reproduce  JSONB NOT NULL DEFAULT '[]',
                             expected_result     TEXT,
                             actual_result       TEXT,
                             severity            bug_severity NOT NULL DEFAULT 'MINOR',
                             status              bug_status NOT NULL DEFAULT 'OPEN',
                             file_path           VARCHAR(500),
                             tags                JSONB DEFAULT '[]',
                             created_at          TIMESTAMP NOT NULL DEFAULT now(),
                             updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

-- INDEXES
CREATE INDEX idx_projects_user_id ON projects(user_id);
CREATE INDEX idx_commits_project_id ON commit_history(project_id);
CREATE INDEX idx_commits_is_bug_fix ON commit_history(project_id, is_bug_fix);
CREATE INDEX idx_file_risks_project_id ON file_risks(project_id);
CREATE INDEX idx_file_risks_score ON file_risks(project_id, risk_score DESC);
CREATE INDEX idx_test_scenarios_project_id ON test_scenarios(project_id);
CREATE INDEX idx_bug_reports_project_id ON bug_reports(project_id);
CREATE INDEX idx_bug_reports_status ON bug_reports(project_id, status);