package com.careflow.authservice.exception;

public class InviteAlreadyPendingException extends RuntimeException {

    public InviteAlreadyPendingException(String email) {
        super("A pending invitation already exists for: " + email);
    }
}
