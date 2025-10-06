-- Initial schema for OLTP database
-- Compatible with SQLite, PostgreSQL, and MySQL

-- Users table
CREATE TABLE users (
    username VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_login TIMESTAMP
);

-- Create index on email for fast lookups
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_enabled ON users(enabled);

-- Insert default admin user (password: admin)
-- Hash: jGl25bVBBBW96Qi9Te4V37Fnqchz/Eu4qB9vKrRIqRg=
INSERT INTO users (username, email, password_hash, created_at, updated_at, enabled)
VALUES ('admin', 'admin@tamarind.local',
        'jGl25bVBBBW96Qi9Te4V37Fnqchz/Eu4qB9vKrRIqRg=',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE);

