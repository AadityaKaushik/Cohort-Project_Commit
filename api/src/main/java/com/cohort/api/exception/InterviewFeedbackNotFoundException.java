package com.cohort.api.exception;

public class InterviewFeedbackNotFoundException extends RuntimeException {
    public InterviewFeedbackNotFoundException() {
        super("No feedback has been recorded for this interview yet.");
    }
}
