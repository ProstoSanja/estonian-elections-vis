-- Create push_subscriptions table
CREATE TABLE push_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    endpoint TEXT NOT NULL UNIQUE,
    p256dh TEXT NOT NULL,
    auth TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Create push subscription topics table for relational topic storage
CREATE TABLE push_subscriptions_topics (
    id BIGSERIAL PRIMARY KEY,
    push_subscription_id BIGINT NOT NULL REFERENCES push_subscriptions(id) ON DELETE CASCADE,
    election_type TEXT NOT NULL,
    topic_type TEXT NOT NULL, -- 'region' or 'candidate'
    topic_code TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(push_subscription_id, election_type, topic_type, topic_code)
);

-- Create indexes for efficient querying
CREATE INDEX idx_topics_subscription_id ON push_subscriptions_topics(push_subscription_id);
CREATE INDEX idx_topics_election_type ON push_subscriptions_topics(election_type);
CREATE INDEX idx_topics_lookup ON push_subscriptions_topics(election_type, topic_type, topic_code);
