package com.careflow.authservice.exception;

public class InviteExpiredException extends RuntimeException {

    public InviteExpiredException() {
        super("Invitation has expired");
    }
}
