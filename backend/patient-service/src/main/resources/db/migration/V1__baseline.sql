CREATE TABLE patients (
    id UUID NOT NULL,
    assigned_doctor_id UUID,
    clinic_id UUID NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL,
    diagnosis VARCHAR(255),
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    CONSTRAINT patients_pkey PRIMARY KEY (id),
    CONSTRAINT patients_status_check CHECK (
        status IN ('ACTIVE', 'AT_RISK', 'INACTIVE')
    )
);
