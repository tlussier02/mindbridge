-- Digital Therapy Assistant Database Schema
-- This schema is managed by Hibernate (ddl-auto=update), this file serves as documentation.

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    onboarding_complete BOOLEAN DEFAULT FALSE,
    onboarding_path VARCHAR(50),
    severity_level VARCHAR(50),
    streak_days INTEGER DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS session_modules (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(255),
    order_index INTEGER
);

CREATE TABLE IF NOT EXISTS cbt_sessions (
    id UUID PRIMARY KEY,
    module_id UUID REFERENCES session_modules(id),
    title VARCHAR(255),
    description TEXT,
    duration_minutes INTEGER,
    order_index INTEGER
);

CREATE TABLE IF NOT EXISTS cbt_session_objectives (
    cbt_session_id UUID REFERENCES cbt_sessions(id),
    objectives VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS cbt_session_modalities (
    cbt_session_id UUID REFERENCES cbt_sessions(id),
    modalities VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    cbt_session_id UUID REFERENCES cbt_sessions(id),
    status VARCHAR(50),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    mood_before INTEGER,
    mood_after INTEGER
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id UUID PRIMARY KEY,
    user_session_id UUID REFERENCES user_sessions(id),
    role VARCHAR(50),
    content TEXT,
    modality VARCHAR(50),
    timestamp TIMESTAMP
);

CREATE TABLE IF NOT EXISTS diary_entries (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    situation TEXT,
    automatic_thought TEXT,
    alternative_thought TEXT,
    mood_before INTEGER,
    mood_after INTEGER,
    belief_rating_before INTEGER,
    belief_rating_after INTEGER,
    created_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS diary_entry_emotions (
    diary_entry_id UUID REFERENCES diary_entries(id),
    emotion VARCHAR(255),
    intensity INTEGER
);

CREATE TABLE IF NOT EXISTS cognitive_distortions (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    description TEXT
);

CREATE TABLE IF NOT EXISTS distortion_examples (
    cognitive_distortion_id VARCHAR(255) REFERENCES cognitive_distortions(id),
    examples VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS diary_entry_distortions (
    diary_entry_id UUID REFERENCES diary_entries(id),
    distortion_id VARCHAR(255) REFERENCES cognitive_distortions(id),
    PRIMARY KEY (diary_entry_id, distortion_id)
);

CREATE TABLE IF NOT EXISTS trusted_contacts (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    name VARCHAR(255),
    phone VARCHAR(50),
    relationship VARCHAR(255)
);
