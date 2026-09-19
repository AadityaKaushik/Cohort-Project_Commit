package com.cohort.api.exception;

/** 404 — the interview exists and is owned by the caller, but no debrief has been saved for it yet. */
public class InterviewFeedbackNotFoundException extends RuntimeException {

    public InterviewFeedbackNotFoundException() {
        super("No feedback has been recorded for this interview yet.");
    }
}
