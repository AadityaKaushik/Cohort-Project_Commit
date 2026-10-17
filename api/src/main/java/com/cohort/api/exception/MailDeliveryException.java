package com.cohort.api.exception;

public class MailDeliveryException extends RuntimeException {
    public MailDeliveryException(Throwable cause) {
        super("We couldn't send the email. Try again in a minute.", cause);
    }
}
