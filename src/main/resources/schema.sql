-- AI Video Intelligence Platform - PostgreSQL Schema
-- This script creates the initial database structure

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE
);

-- Videos table
CREATE TABLE IF NOT EXISTS videos (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    source_url VARCHAR(2000) NOT NULL,
    source VARCHAR(50) NOT NULL,
    thumbnail_url VARCHAR(500),
    duration_seconds BIGINT,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    processing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message VARCHAR(2000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE
);

-- Transcript chunks table
CREATE TABLE IF NOT EXISTS transcript_chunks (
    id BIGSERIAL PRIMARY KEY,
    video_id BIGINT NOT NULL REFERENCES videos(id) ON DELETE CASCADE,
    chunk_text TEXT NOT NULL,
    start_time INTEGER NOT NULL,
    end_time INTEGER NOT NULL,
    speaker VARCHAR(255),
    topic VARCHAR(255),
    embedding TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE
);

-- Summaries table
CREATE TABLE IF NOT EXISTS summaries (
    id BIGSERIAL PRIMARY KEY,
    video_id BIGINT NOT NULL UNIQUE REFERENCES videos(id) ON DELETE CASCADE,
    summary_text TEXT NOT NULL,
    key_points TEXT,
    summary_type VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE
);

-- Action items table
CREATE TABLE IF NOT EXISTS action_items (
    id BIGSERIAL PRIMARY KEY,
    video_id BIGINT NOT NULL REFERENCES videos(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    assigned_to VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    time_reference INTEGER,
    priority VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_videos_user_id ON videos(user_id);
CREATE INDEX IF NOT EXISTS idx_videos_status ON videos(processing_status);
CREATE INDEX IF NOT EXISTS idx_transcript_chunks_video_id ON transcript_chunks(video_id);
CREATE INDEX IF NOT EXISTS idx_transcript_chunks_start_time ON transcript_chunks(start_time);
CREATE INDEX IF NOT EXISTS idx_action_items_video_id ON action_items(video_id);
CREATE INDEX IF NOT EXISTS idx_action_items_status ON action_items(status);
