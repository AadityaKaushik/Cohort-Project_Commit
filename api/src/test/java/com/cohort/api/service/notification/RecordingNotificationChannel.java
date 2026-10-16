package com.cohort.api.service.notification;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Test channel that records what it delivered, deduplicating by key. */
class RecordingNotificationChannel implements NotificationChannel {

    private final String id;
    final List<NotificationMessage> delivered = new ArrayList<>();
    private final Set<String> seenKeys = new HashSet<>();

    RecordingNotificationChannel(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean deliver(NotificationMessage message) {
        if (message.dedupeKey() != null && !seenKeys.add(message.userId() + "|" + message.dedupeKey())) {
            return false;
        }
        delivered.add(message);
        return true;
    }
}
