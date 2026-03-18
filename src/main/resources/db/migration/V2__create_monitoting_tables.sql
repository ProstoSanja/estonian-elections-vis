CREATE TABLE IF NOT EXISTS monitoring_individual (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    firstname TEXT,
    surname TEXT,
    birthdate DATE NOT NULL, -- dropped at end of migration
    external_id_isikukood TEXT UNIQUE,
    external_id_ariregister TEXT UNIQUE
);

CREATE TABLE IF NOT EXISTS monitoring_group (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    external_id_ariregister TEXT UNIQUE
);

CREATE TABLE IF NOT EXISTS monitoring_group_membership (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES monitoring_group(id) ON DELETE CASCADE,
    individual_id BIGINT NOT NULL REFERENCES monitoring_individual(id) ON DELETE CASCADE,
    position TEXT,
    position_of_authority BOOLEAN NOT NULL DEFAULT FALSE,
    -- position_income BIGINT, -- added at end of migration
    -- position_confirmed BOOLEAN NOT NULL, -- added at end of migration
    start_date DATE NOT NULL,
    end_date DATE
);

CREATE TABLE IF NOT EXISTS monitoring_group_relationship (
    id BIGSERIAL PRIMARY KEY,
    parent_group_id BIGINT NOT NULL REFERENCES monitoring_group(id) ON DELETE CASCADE,
    child_group_id BIGINT NOT NULL REFERENCES monitoring_group(id) ON DELETE CASCADE,
    UNIQUE (parent_group_id, child_group_id)
);

CREATE INDEX IF NOT EXISTS idx_group_membership_group_id ON monitoring_group_membership(group_id);
CREATE INDEX IF NOT EXISTS idx_group_membership_individual_id ON monitoring_group_membership(individual_id);
CREATE INDEX IF NOT EXISTS idx_group_membership_group_id_individual_id ON monitoring_group_membership(group_id, individual_id);
CREATE INDEX IF NOT EXISTS idx_group_relationship_parent_group_id ON monitoring_group_relationship(parent_group_id);
CREATE INDEX IF NOT EXISTS idx_group_relationship_child_group_id ON monitoring_group_relationship(child_group_id);
CREATE INDEX IF NOT EXISTS idx_group_relationship_parent_group_id_child_group_id ON monitoring_group_relationship(parent_group_id, child_group_id);

ALTER TABLE monitoring_individual ALTER COLUMN birthdate DROP NOT NULL;
ALTER TABLE monitoring_group_membership ADD COLUMN position_income BIGINT;
ALTER TABLE monitoring_group_membership ADD COLUMN position_confirmed BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE monitoring_group_membership ALTER COLUMN position_confirmed DROP DEFAULT;