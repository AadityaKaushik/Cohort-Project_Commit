package com.cohort.api.exception;

/** The sign-in cannot be attached to an account: the provider did not vouch for the email address. */
public class OAuthLinkRefusedException extends RuntimeException {

    public enum Reason { UNVERIFIED_EMAIL, MISSING_EMAIL }

    private final Reason reason;

    public OAuthLinkRefusedException(Reason reason) {
        super(reason == Reason.UNVERIFIED_EMAIL
                ? "The provider has not verified this email address, so it cannot be linked to an existing account."
                : "The provider did not share an email address.");
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
