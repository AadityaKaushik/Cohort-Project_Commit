package com.cohort.api.service.ai;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Rejects model replies that are too short, too long or that bring in numbers the user never gave. */
public final class DraftOutput {
    private static final Pattern NUMBER = Pattern.compile("\\d[\\d,.]*\\d|\\d");

    private DraftOutput() {
    }

    public static String accept(String reply, String reference, int minLength) {
        if (reply == null) {
            return null;
        }
        String text = reply.strip();
        if (text.length() < minLength || text.length() > 4000) {
            return null;
        }
        Set<String> allowed = numbersIn(reference);
        return allowed.containsAll(numbersIn(text)) ? text : null;
    }

    private static Set<String> numbersIn(String text) {
        Set<String> numbers = new HashSet<>();
        Matcher matcher = NUMBER.matcher(text);
        while (matcher.find()) {
            numbers.add(matcher.group().replaceAll("[.,]+$", ""));
        }
        return numbers;
    }
}
