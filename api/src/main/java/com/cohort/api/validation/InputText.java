package com.cohort.api.validation;

public final class InputText {
    private InputText() {
    }

    public static String collapse(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[\\p{Z}\\s]+", " ").trim();
    }
}
