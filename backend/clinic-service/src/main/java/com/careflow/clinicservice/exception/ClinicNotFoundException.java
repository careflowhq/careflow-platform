package com.careflow.clinicservice.exception;

import java.util.UUID;

public class ClinicNotFoundException extends RuntimeException {

    public ClinicNotFoundException(UUID id) {
        super("Clinic not found: " + id);
    }
}
