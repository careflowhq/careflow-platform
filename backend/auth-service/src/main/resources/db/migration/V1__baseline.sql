CREATE TABLE users (
    id UUID NOT NULL,
    clinic_id UUID,
    email VARCHAR(255),
    full_name VARCHAR(255),
    password_hash VARCHAR(255),
    role VARCHAR(255),
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT users_role_check CHECK (
        role IN ('PLATFORM_ADMIN', 'CLINIC_ADMIN', 'DOCTOR', 'ASSISTANT')
    )
);

CREATE TABLE invitations (
    id UUID NOT NULL,
    clinic_id UUID NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL,
    email VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ(6) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    invited_by UUID NOT NULL,
    role VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL,
    CONSTRAINT invitations_pkey PRIMARY KEY (id),
    CONSTRAINT uk_invitations_token UNIQUE (token),
    CONSTRAINT invitations_role_check CHECK (
        role IN ('PLATFORM_ADMIN', 'CLINIC_ADMIN', 'DOCTOR', 'ASSISTANT')
    ),
    CONSTRAINT invitations_status_check CHECK (
        status IN ('PENDING', 'ACCEPTED', 'EXPIRED')
    )
);

CREATE INDEX idx_invitations_clinic_email ON invitations (clinic_id, email);
