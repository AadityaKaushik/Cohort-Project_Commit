package com.cohort.api.service.notification;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A stand-in delivery strategy (think "email") that just remembers what it delivered, deduplicating by key like any real channel must. */
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
