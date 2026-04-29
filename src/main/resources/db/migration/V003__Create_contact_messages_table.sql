-- Create contact_messages table

CREATE TABLE IF NOT EXISTS contact_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    company VARCHAR(255),
    subject VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    replied_at TIMESTAMP,
    admin_notes TEXT,

    CONSTRAINT check_status CHECK (status IN ('NEW', 'READ', 'REPLIED', 'ARCHIVED'))
);

-- Create indexes for better query performance
CREATE INDEX idx_contact_messages_email ON contact_messages(email);
CREATE INDEX idx_contact_messages_status ON contact_messages(status);
CREATE INDEX idx_contact_messages_created_at ON contact_messages(created_at DESC);
CREATE INDEX idx_contact_messages_updated_at ON contact_messages(updated_at DESC);

-- Add comment to table
COMMENT ON TABLE contact_messages IS 'Stores contact form submissions from users';
COMMENT ON COLUMN contact_messages.status IS 'Message status: NEW, READ, REPLIED, or ARCHIVED';
COMMENT ON COLUMN contact_messages.ip_address IS 'IP address of the requester for spam detection';
COMMENT ON COLUMN contact_messages.admin_notes IS 'Internal notes for admin responses';
