package com.cohort.api.service.detection;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;

/** A publisher with no subscribers at all -- it only remembers what it was handed, which is all a detector test needs to see. */
final class RecordingEventPublisher implements ApplicationEventPublisher {

    final List<Object> published = new ArrayList<>();

    @Override
    public void publishEvent(Object event) {
        published.add(event);
    }

    <T> List<T> ofType(Class<T> type) {
        return published.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
