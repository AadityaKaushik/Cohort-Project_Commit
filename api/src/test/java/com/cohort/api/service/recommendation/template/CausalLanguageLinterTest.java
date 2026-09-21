package com.cohort.api.service.recommendation.template;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for the checker itself, independent of any real template -- see {@link RecommendationTemplateRegistryTest#test_no_causal_language} for the checker applied to the actual registry. */
class CausalLanguageLinterTest {

    @Test
    void catchesEachBannedPhraseCaseInsensitively() {
        assertThat(CausalLanguageLinter.findViolations("This GUARANTEES a better outcome.")).contains("guarantees");
        assertThat(CausalLanguageLinter.findViolations("Practice will get you the offer.")).contains("will get you");
        assertThat(CausalLanguageLinter.findViolations("It ensures success.")).contains("ensures");
        assertThat(CausalLanguageLinter.findViolations("This caused your rejection.")).contains("caused");
        assertThat(CausalLanguageLinter.findViolations("This is proven to work.")).contains("proven to");
        assertThat(CausalLanguageLinter.findViolations("Weak prep always leads to failure.")).contains("always leads to");
        assertThat(CausalLanguageLinter.findViolations("You failed because you didn't study.")).contains("you failed because");
        assertThat(CausalLanguageLinter.findViolations("Skipping practice led to the loss.")).contains("led to");
        assertThat(CausalLanguageLinter.findViolations("You were rejected because you froze.")).contains("because you");
    }

    @Test
    void findsNoViolationsInCleanAssociativeText() {
        String clean = "Mastery is at 30%, based on 5 observations, associated with lower self-rated confidence on related questions.";

        assertThat(CausalLanguageLinter.findViolations(clean)).isEmpty();
    }

    @Test
    void treatsNullAndBlankTextAsClean() {
        assertThat(CausalLanguageLinter.findViolations(null)).isEmpty();
        assertThat(CausalLanguageLinter.findViolations("   ")).isEmpty();
    }
}
