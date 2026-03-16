CREATE TABLE IF NOT EXISTS monitoring_individual (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    firstname TEXT,
    surname TEXT,
    birthdate DATE NOT NULL,
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
    start_date DATE NOT NULL,
    end_date DATE
);

CREATE INDEX IF NOT EXISTS idx_group_membership_group_id ON monitoring_group_membership(group_id);
CREATE INDEX IF NOT EXISTS idx_group_membership_individual_id ON monitoring_group_membership(individual_id);
CREATE INDEX IF NOT EXISTS idx_group_membership_group_id_individual_id ON monitoring_group_membership(group_id, individual_id);