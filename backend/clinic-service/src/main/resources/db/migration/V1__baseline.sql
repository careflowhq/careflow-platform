CREATE TABLE clinics (
    id UUID NOT NULL,
    active BOOLEAN NOT NULL,
    country VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    subscription_plan VARCHAR(255) NOT NULL,
    timezone VARCHAR(255) NOT NULL,
    CONSTRAINT clinics_pkey PRIMARY KEY (id),
    CONSTRAINT clinics_subscription_plan_check CHECK (
        subscription_plan IN ('FREE', 'BASIC', 'PRO')
    )
);
