CREATE TABLE notifications (
    id UUID NOT NULL,
    channel VARCHAR(255) NOT NULL,
    clinic_id UUID NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL,
    delivery_url VARCHAR(255),
    event_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    follow_up_id UUID,
    message VARCHAR(2000) NOT NULL,
    patient_id UUID,
    recipient_name VARCHAR(255),
    recipient_phone VARCHAR(255),
    sent_at TIMESTAMPTZ(6),
    status VARCHAR(255) NOT NULL,
    CONSTRAINT notifications_pkey PRIMARY KEY (id),
    CONSTRAINT uk_notifications_event_id UNIQUE (event_id),
    CONSTRAINT notifications_channel_check CHECK (
        channel IN ('WHATSAPP_LINK', 'LOG')
    ),
    CONSTRAINT notifications_status_check CHECK (
        status IN ('PENDING', 'READY', 'SENT', 'FAILED')
    )
);

CREATE INDEX idx_notifications_clinic_created ON notifications (clinic_id, created_at);
