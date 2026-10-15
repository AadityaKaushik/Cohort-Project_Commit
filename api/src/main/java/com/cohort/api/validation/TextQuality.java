package com.cohort.api.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Rule-based checks for free text. Hard problems reject the input, hints only warn. */
public final class TextQuality {
    public enum Problem {
        EMPTY("Write something, or leave this blank."),
        CONTROL_CHARACTERS("This contains characters that cannot be saved. Remove them and try again."),
        NO_LETTERS("Add a few words. This has no letters in it."),
        TOO_FEW_LETTERS("This is mostly symbols or numbers. Add a few words."),
        REPEATED_CHARACTER("This repeats one character many times. Describe the round, the questions or the outcome."),
        REPEATED_WORD("This repeats the same word. Describe the round, the questions or the outcome."),
        KEYBOARD_MASH("This looks like random typing. Describe the round, the questions or the outcome.");

        private final String message;

        Problem(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    public enum Hint {
        OFF_TOPIC("This does not look like it is about the interview or the company. You can still save it."),
        CONTAINS_CONTACT_DETAILS("This seems to include an email address or phone number. Check it is yours to store."),
        STOPWORDS_ONLY("This has only filler words. Add what happened or what was asked.");

        private final String message;

        Hint(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    static final int MAX_IDENTICAL_RUN = 8;
    static final double MIN_LETTER_RATIO = 0.15;
    static final int RATIO_MIN_LENGTH = 10;
    static final int REPEATED_WORD_MIN_TOKENS = 4;
    static final int OFF_TOPIC_MIN_WORDS = 5;
    static final int MASH_MIN_LENGTH = 6;
    static final double MASH_NEIGHBOUR_RATIO = 0.6;

    private static final String[] KEYBOARD_ROWS = {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
    private static final List<String> MASH_FRAGMENTS =
            List.of("asdf", "qwer", "zxcv", "hjkl", "uiop", "sdfg", "dfgh", "fghj");
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^\\p{L}\\p{N}']+");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)\\+?\\d[\\d\\s().-]{7,}\\d(?!\\d)");
    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were", "be", "been", "it", "its", "this",
            "that", "these", "those", "of", "to", "in", "on", "at", "for", "with", "as", "by", "from", "i", "we",
            "you", "he", "she", "they", "me", "my", "so", "very", "just", "not", "no", "yes", "do", "did", "had",
            "has", "have", "there", "then", "than", "if");
    private static final Set<String> TOPIC_WORDS = Set.of(
            "hr", "cv", "dsa", "ctc", "sql", "pay", "job", "role", "team", "test", "code", "easy", "hard", "hike",
            "java", "task", "case", "panel", "stage", "screen", "online", "system", "design", "growth", "office",
            "remote", "hybrid", "intern");
    private static final List<String> TOPIC_PREFIXES = List.of(
            "interview", "question", "round", "offer", "reject", "feedback", "recruit", "salar", "compens",
            "benefit", "cultur", "manag", "technical", "behavio", "assess", "assign", "algorithm", "coding",
            "process", "outcome", "result", "difficult", "onboard", "hiring", "work", "compan", "colleag",
            "prepar", "topic", "answer", "project", "resume", "mentor", "stipend", "packag", "select",
            "onsite", "discuss", "balanc", "leetcode", "python", "react", "applic", "shortlist", "ghost",
            "explain", "approach", "solution", "solv", "stuck", "nervous", "confiden", "communicat", "mistake",
            "concept", "understand", "complexit", "optimi", "debug", "edge", "clear", "struggl", "time");

    private TextQuality() {
    }

    public static Optional<Problem> problem(String text) {
        if (text == null || text.isBlank()) {
            return Optional.of(Problem.EMPTY);
        }
        if (hasControlCharacters(text)) {
            return Optional.of(Problem.CONTROL_CHARACTERS);
        }
        int letters = 0;
        int nonSpace = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (!Character.isWhitespace(cp) && !Character.isSpaceChar(cp)) {
                nonSpace++;
                if (Character.isLetter(cp)) {
                    letters++;
                }
            }
        }
        if (letters == 0) {
            return Optional.of(Problem.NO_LETTERS);
        }
        if (nonSpace >= RATIO_MIN_LENGTH && (double) letters / nonSpace < MIN_LETTER_RATIO) {
            return Optional.of(Problem.TOO_FEW_LETTERS);
        }
        if (longestIdenticalRun(text) >= MAX_IDENTICAL_RUN) {
            return Optional.of(Problem.REPEATED_CHARACTER);
        }
        List<String> tokens = tokens(text);
        if (isRepeatedWord(tokens)) {
            return Optional.of(Problem.REPEATED_WORD);
        }
        if (isMash(tokens)) {
            return Optional.of(Problem.KEYBOARD_MASH);
        }
        return Optional.empty();
    }

    public static List<Hint> hints(String text, boolean experienceField) {
        List<Hint> hints = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return hints;
        }
        if (EMAIL.matcher(text).find() || containsPhoneNumber(text)) {
            hints.add(Hint.CONTAINS_CONTACT_DETAILS);
        }
        List<String> tokens = tokens(text);
        if (tokens.size() >= 3 && tokens.stream().allMatch(STOPWORDS::contains)) {
            hints.add(Hint.STOPWORDS_ONLY);
        } else if (experienceField && tokens.size() >= OFF_TOPIC_MIN_WORDS && isLatin(text)
                && tokens.stream().noneMatch(TextQuality::isTopicWord)) {
            hints.add(Hint.OFF_TOPIC);
        }
        return hints;
    }

    static boolean hasControlCharacters(String text) {
        return text.codePoints().anyMatch(cp -> Character.isISOControl(cp) && cp != '\n' && cp != '\r' && cp != '\t');
    }

    static int longestIdenticalRun(String text) {
        int longest = 0;
        int run = 0;
        int previous = -1;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (!Character.isLetterOrDigit(cp)) {
                run = 0;
                previous = -1;
                continue;
            }
            run = cp == previous ? run + 1 : 1;
            previous = cp;
            longest = Math.max(longest, run);
        }
        return longest;
    }

    private static List<String> tokens(String text) {
        return Arrays.stream(TOKEN_SPLIT.split(text.toLowerCase(Locale.ROOT)))
                .filter(token -> !token.isEmpty())
                .toList();
    }

    private static boolean isRepeatedWord(List<String> tokens) {
        if (tokens.size() >= REPEATED_WORD_MIN_TOKENS && tokens.stream().distinct().count() == 1) {
            return true;
        }
        return tokens.size() == 1 && isRepeatedUnit(tokens.get(0));
    }

    private static boolean isRepeatedUnit(String token) {
        if (token.length() < 6) {
            return false;
        }
        for (int unit = 1; unit <= 4; unit++) {
            if (token.length() % unit == 0 && token.length() / unit >= 3
                    && token.equals(token.substring(0, unit).repeat(token.length() / unit))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMash(List<String> tokens) {
        if (tokens.isEmpty()) {
            return false;
        }
        long mashed = tokens.stream().filter(TextQuality::isMashToken).count();
        return mashed > 0 && mashed * 2 >= tokens.size();
    }

    private static boolean isMashToken(String token) {
        if (!token.chars().allMatch(c -> c >= 'a' && c <= 'z')) {
            return false;
        }
        if (token.length() >= 4 && MASH_FRAGMENTS.stream().anyMatch(token::contains)) {
            return true;
        }
        if (token.length() < MASH_MIN_LENGTH) {
            return false;
        }
        int neighbours = 0;
        for (int i = 0; i < token.length() - 1; i++) {
            if (areNeighbours(token.charAt(i), token.charAt(i + 1))) {
                neighbours++;
            }
        }
        return (double) neighbours / (token.length() - 1) >= MASH_NEIGHBOUR_RATIO;
    }

    private static boolean areNeighbours(char a, char b) {
        for (String row : KEYBOARD_ROWS) {
            int first = row.indexOf(a);
            int second = row.indexOf(b);
            if (first >= 0 && second >= 0) {
                return Math.abs(first - second) == 1;
            }
        }
        return false;
    }

    private static boolean containsPhoneNumber(String text) {
        Matcher matcher = PHONE.matcher(text);
        while (matcher.find()) {
            if (matcher.group().chars().filter(Character::isDigit).count() >= 9) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLatin(String text) {
        return text.codePoints()
                .filter(Character::isLetter)
                .allMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.LATIN);
    }

    private static boolean isTopicWord(String token) {
        return TOPIC_WORDS.contains(token) || TOPIC_PREFIXES.stream().anyMatch(token::startsWith);
    }
}
