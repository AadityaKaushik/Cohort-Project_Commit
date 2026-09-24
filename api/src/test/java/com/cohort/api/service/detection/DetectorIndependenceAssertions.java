package com.cohort.api.service.detection;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;

/**
 * The structural half of P59's "the detector doesn't know who listens":
 * a detector may depend on data sources and on Spring's generic
 * {@link ApplicationEventPublisher}, but on nothing from the notification
 * side (or any other subscriber) -- checked against the compiled class
 * itself, so a future change that wires a listener straight into a detector
 * fails this test rather than slipping through review.
 */
final class DetectorIndependenceAssertions {

    private static final List<String> SUBSCRIBER_PACKAGES = List.of(
            "com.cohort.api.service.notification",
            "com.cohort.api.service.report");

    private DetectorIndependenceAssertions() {
    }

    static void assertKnowsNoSubscriber(Class<?> detector) {
        for (Field field : detector.getDeclaredFields()) {
            assertNotASubscriberType(detector, field.getType());
        }
        for (Constructor<?> constructor : detector.getDeclaredConstructors()) {
            Arrays.stream(constructor.getParameterTypes()).forEach(type -> assertNotASubscriberType(detector, type));
        }
        boolean publishesGenerically = Arrays.stream(detector.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(ApplicationEventPublisher.class));
        assertThat(publishesGenerically)
                .as("%s must publish through Spring's generic ApplicationEventPublisher", detector.getSimpleName())
                .isTrue();
    }

    private static void assertNotASubscriberType(Class<?> detector, Class<?> type) {
        String packageName = type.getPackageName();
        assertThat(SUBSCRIBER_PACKAGES)
                .as("%s depends on %s, which belongs to a subscriber", detector.getSimpleName(), type.getName())
                .noneMatch(packageName::startsWith);
        assertThat(type.getSimpleName())
                .as("%s depends on %s -- a detector must not know its listeners", detector.getSimpleName(), type.getName())
                .doesNotContain("Listener")
                .doesNotContain("Notification");
    }
}
