-- Extensions for fuzzy name search
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Immutable wrapper for unaccent (needed for indexing; unaccent itself is STABLE, not IMMUTABLE)
CREATE OR REPLACE FUNCTION f_unaccent(text) RETURNS text AS $$
    SELECT public.unaccent('public.unaccent', $1)
$$ LANGUAGE sql IMMUTABLE PARALLEL SAFE;

-- ============================================================
-- TABLES
-- ============================================================

CREATE TABLE IF NOT EXISTS monitoring_entry (
    id BIGSERIAL PRIMARY KEY,
    type TEXT NOT NULL,
    name TEXT NOT NULL,
    birth_date DATE,
    extra JSONB DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS monitoring_entry_external_id (
    id BIGSERIAL PRIMARY KEY,
    entry_id BIGINT NOT NULL REFERENCES monitoring_entry(id) ON DELETE CASCADE,
    type TEXT NOT NULL,
    value TEXT NOT NULL,
    UNIQUE (type, value)
);

CREATE TABLE IF NOT EXISTS monitoring_entry_name (
    id BIGSERIAL PRIMARY KEY,
    entry_id BIGINT NOT NULL REFERENCES monitoring_entry(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    first_name TEXT,
    last_name TEXT,
    business_name TEXT,
    business_suffix TEXT,
    start_date DATE,
    end_date DATE
);

CREATE TABLE IF NOT EXISTS monitoring_connection (
    id BIGSERIAL PRIMARY KEY,
    type TEXT NOT NULL,
    name TEXT NOT NULL,
    confirmed BOOLEAN NOT NULL DEFAULT false,
    entry_a_id BIGINT NOT NULL REFERENCES monitoring_entry(id),
    entry_b_id BIGINT REFERENCES monitoring_entry(id),
    parent_id BIGINT REFERENCES monitoring_entry(id),
    monetary_value NUMERIC,
    start_date DATE,
    end_date DATE,
    sources JSONB DEFAULT '[]'
);

CREATE TABLE IF NOT EXISTS monitoring_connection_candidate (
    connection_id BIGINT NOT NULL REFERENCES monitoring_connection(id) ON DELETE CASCADE,
    entry_id BIGINT NOT NULL REFERENCES monitoring_entry(id),
    PRIMARY KEY (connection_id, entry_id)
);

-- ============================================================
-- INDEXES
-- ============================================================

-- Entry lookups
CREATE INDEX IF NOT EXISTS idx_me_type ON monitoring_entry(type);
CREATE INDEX IF NOT EXISTS idx_me_lower_name ON monitoring_entry(lower(name));
CREATE INDEX IF NOT EXISTS idx_me_birth_date ON monitoring_entry(birth_date) WHERE birth_date IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_me_name_trgm ON monitoring_entry USING GIN (f_unaccent(name) gin_trgm_ops);

-- External ID lookups (UNIQUE constraint covers type+value; add entry_id index)
CREATE INDEX IF NOT EXISTS idx_meid_entry ON monitoring_entry_external_id(entry_id);

-- Name lookups
CREATE INDEX IF NOT EXISTS idx_men_entry ON monitoring_entry_name(entry_id);
CREATE INDEX IF NOT EXISTS idx_men_lower_first_last ON monitoring_entry_name(lower(first_name), lower(last_name));
CREATE INDEX IF NOT EXISTS idx_men_lower_full ON monitoring_entry_name(lower(full_name));
CREATE INDEX IF NOT EXISTS idx_men_full_trgm ON monitoring_entry_name USING GIN (f_unaccent(full_name) gin_trgm_ops);

-- Connection graph traversal
CREATE INDEX IF NOT EXISTS idx_mc_entry_a ON monitoring_connection(entry_a_id);
CREATE INDEX IF NOT EXISTS idx_mc_entry_b ON monitoring_connection(entry_b_id) WHERE entry_b_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_mc_type ON monitoring_connection(type);
CREATE INDEX IF NOT EXISTS idx_mc_upsert ON monitoring_connection(entry_a_id, entry_b_id, type, start_date);

-- Candidate resolution
CREATE INDEX IF NOT EXISTS idx_mcc_entry ON monitoring_connection_candidate(entry_id);

-- ============================================================
-- CONVENIENCE VIEW: all connections for a given entity
-- ============================================================

CREATE OR REPLACE VIEW monitoring_entity_connections AS
    SELECT c.*, e.id AS entity_id, 'a' AS side
    FROM monitoring_connection c
    JOIN monitoring_entry e ON e.id = c.entry_a_id
UNION ALL
    SELECT c.*, e.id AS entity_id, 'b' AS side
    FROM monitoring_connection c
    JOIN monitoring_entry e ON e.id = c.entry_b_id
UNION ALL
    SELECT c.*, cc.entry_id AS entity_id, 'candidate' AS side
    FROM monitoring_connection c
    JOIN monitoring_connection_candidate cc ON cc.connection_id = c.id;
