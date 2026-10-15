package com.cohort.api.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.validation.TextQuality.Hint;
import com.cohort.api.validation.TextQuality.Problem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TextQualityTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "Rejected after round 2",
        "No feedback yet",
        "Offer accepted",
        "Two coding rounds and a system design round. They asked about caching and database indexes.",
        "The recruiter called back within a week and the HR round was relaxed.",
        "The tea there was not good",
        "Great team, but the commute is long.",
        "O(n log n) with a heap",
        "arr[i] += 1; // O(n)",
        "Entretien technique en deux parties, questions sur Java et les bases de donnees.",
        "Das Vorstellungsgespraech war sehr freundlich und dauerte eine Stunde.",
        "इंटरव्यू में दो राउंड थे और सवाल कोडिंग पर थे",
        "面试一共三轮，问了很多算法题",
        "Собеседование прошло хорошо, спрашивали про базы данных",
        "مقابلة العمل كانت جيدة وسألوا عن البرمجة",
        "Hmmmm not sure how it went",
        "Wooooow what a long day -------- but good",
        "Typewriter pretty puppy poetry",
        "Round 1: DSA\nRound 2: HLD\nRound 3: HR",
        "ok",
        "Explained my approach clearly",
        "Salary 18 LPA, joining in 60 days"
    })
    void legitimateTextPasses(String text) {
        assertThat(TextQuality.problem(text)).as(text).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "'   '|EMPTY",
        "😂😂😂😂😂|NO_LETTERS",
        "12345 6789|NO_LETTERS",
        "!!!! ???? ....|NO_LETTERS",
        "!!!!!!!!!!!!!!!!! a|TOO_FEW_LETTERS",
        "aaaaaaaaaa|REPEATED_CHARACTER",
        "good gooooooooood|REPEATED_CHARACTER",
        "test test test test|REPEATED_WORD",
        "asdfasdfasdf|REPEATED_WORD",
        "hahahahahaha|REPEATED_WORD",
        "asdfghjkl|KEYBOARD_MASH",
        "qwertyuiop|KEYBOARD_MASH",
        "asdf jkl; asdf|KEYBOARD_MASH",
        "sdfsdf zxcvbnm|KEYBOARD_MASH",
        "lkjhgfdsa|KEYBOARD_MASH"
    })
    void junkIsRejected(String text, Problem expected) {
        assertThat(TextQuality.problem(text)).as(text).contains(expected);
    }

    @Test
    void controlCharactersAreRejectedButNewlinesAreFine() {
        assertThat(TextQuality.problem("round one\u0000 done")).contains(Problem.CONTROL_CHARACTERS);
        assertThat(TextQuality.problem("round one\n\tdone")).isEmpty();
    }

    @Test
    void oneOddWordInARealSentenceIsNotMash() {
        assertThat(TextQuality.problem("They asked about asdfgh in the second round of the interview")).isEmpty();
    }

    @Test
    void offTopicExperienceTextGetsAWarning() {
        assertThat(TextQuality.hints("the tea there was not good", true)).containsExactly(Hint.OFF_TOPIC);
        assertThat(TextQuality.hints("the tea there was not good", false)).isEmpty();
    }

    @Test
    void onTopicAndShortTextGetsNoWarning() {
        assertThat(TextQuality.hints("Rejected after round 2", true)).isEmpty();
        assertThat(TextQuality.hints("Two rounds, mostly questions about Java", true)).isEmpty();
        assertThat(TextQuality.hints("Explained my approach clearly", true)).isEmpty();
        assertThat(TextQuality.hints("Good", true)).isEmpty();
    }

    @Test
    void nonLatinTextIsNeverCalledOffTopic() {
        assertThat(TextQuality.hints("मुझे वहां की चाय अच्छी नहीं लगी बिल्कुल भी", true)).isEmpty();
    }

    @Test
    void contactDetailsAreFlagged() {
        assertThat(TextQuality.hints("Round 1 with priya@acme.com went fine", true))
                .contains(Hint.CONTAINS_CONTACT_DETAILS);
        assertThat(TextQuality.hints("Recruiter number +91 98765 43210, round 2", true))
                .contains(Hint.CONTAINS_CONTACT_DETAILS);
        assertThat(TextQuality.hints("Round 2 on 2024-05-12", true)).isEmpty();
    }

    @Test
    void fillerOnlyTextGetsAWarning() {
        assertThat(TextQuality.hints("it is the and this", false)).containsExactly(Hint.STOPWORDS_ONLY);
    }
}
