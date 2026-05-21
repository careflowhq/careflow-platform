package com.careflow.followupservice.exception;

import java.util.UUID;

public class FollowUpNotFoundException extends RuntimeException {

    public FollowUpNotFoundException(UUID id) {
        super("Follow-up not found: " + id);
    }
}
