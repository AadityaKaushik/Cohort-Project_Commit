package com.cohort.api.exception;

/**
 * 404 — the resume itself exists and is owned by the caller, but no file has
 * been uploaded to it yet. Distinct from {@link ResumeNotFoundException} so
 * "no file yet" is diagnosable without ever distinguishing, for a resume the
 * caller does not own, "exists with no file" from "does not exist" (both
 * still surface as {@code ResumeNotFoundException} first).
 */
public class ResumeFileNotFoundException extends RuntimeException {

    public ResumeFileNotFoundException() {
        super("This resume has no file uploaded yet.");
    }
}
