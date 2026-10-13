package com.cohort.api.exception;

import com.cohort.api.entity.ApplicationStatus;
import java.util.Arrays;
import java.util.stream.Collectors;

/** FR-21: an illegal (from, to) status transition, per ApplicationStatusStateRegistry. */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(ApplicationStatus from, ApplicationStatus to) {
        super("An application cannot move from " + label(from) + " to " + label(to) + ".");
    }

    private InvalidStatusTransitionException(String message) {
        super(message);
    }

    /** The move is a backward correction that is only legal with an explanatory note. */
    public static InvalidStatusTransitionException correctionNoteRequired(ApplicationStatus to) {
        return new InvalidStatusTransitionException(
                "Moving back to " + label(to) + " is a correction. Add a note explaining why and try again.");
    }

    private static String label(ApplicationStatus status) {
        return Arrays.stream(status.name().split("_"))
                .map(word -> status == ApplicationStatus.OA ? word : word.charAt(0) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
