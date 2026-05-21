package com.careflow.authservice.client;

public class ClinicServiceException extends RuntimeException {

    public ClinicServiceException(String message) {
        super(message);
    }

    public ClinicServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
