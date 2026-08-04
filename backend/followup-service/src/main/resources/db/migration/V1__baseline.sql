CREATE TABLE followups (
    id UUID NOT NULL,
    clinic_id UUID NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL,
    created_by UUID NOT NULL,
    doctor_id UUID,
    notes VARCHAR(255),
    patient_id UUID NOT NULL,
    scheduled_date TIMESTAMPTZ(6) NOT NULL,
    status VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    CONSTRAINT followups_pkey PRIMARY KEY (id),
    CONSTRAINT followups_status_check CHECK (
        status IN ('PENDING', 'COMPLETED', 'MISSED', 'CANCELLED')
    )
);

CREATE INDEX idx_followups_clinic_status ON followups (clinic_id, status);
CREATE INDEX idx_followups_clinic_scheduled ON followups (clinic_id, scheduled_date);
