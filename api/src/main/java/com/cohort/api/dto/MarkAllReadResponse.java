package com.cohort.api.dto;

/** US-79: how many notifications "mark all as read" actually changed (0 when everything was already read). */
public record MarkAllReadResponse(int markedRead) {
}
