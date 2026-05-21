package com.careflow.authservice.exception;

public class InviteNotFoundException extends RuntimeException {

    public InviteNotFoundException() {
        super("Invitation not found or already used");
    }
}
