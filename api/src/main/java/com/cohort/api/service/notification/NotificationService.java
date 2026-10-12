package com.cohort.api.service.notification;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * The Strategy pattern's context (ARCHITECTURE_REVISION_JAVA.md §4.1/§4.3,
 * NFR-53): receives a channel-agnostic {@link NotificationMessage} and hands
 * it to whichever {@link NotificationChannel} strategies are enabled by
 * {@code app.notifications.channels} -- {@code in_app} by default. It only
 * ever sees the interface: adding an email channel is a new
 * {@code NotificationChannel} bean plus a config value, with no edit here,
 * in {@link NotificationListener}, or in any detector.
 *
 * <p>A failing channel is logged and skipped rather than propagated: one
 * broken delivery route must not stop the others, and must never abort the
 * detection run that published the originating event.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final List<NotificationChannel> activeChannels;

    public NotificationService(
            List<NotificationChannel> availableChannels,
            @Value("${app.notifications.channels:in_app}") List<String> enabledChannelIds) {
        Map<String, NotificationChannel> byId = availableChannels.stream()
                .collect(Collectors.toMap(NotificationChannel::id, channel -> channel, (a, b) -> {
                    throw new IllegalStateException("Two notification channels share the id '" + a.id() + "'");
                }, LinkedHashMap::new));
        Set<String> unknown = enabledChannelIds.stream().filter(id -> !byId.containsKey(id)).collect(Collectors.toSet());
        if (!unknown.isEmpty()) {
            throw new IllegalStateException("app.notifications.channels names unknown channel(s) " + unknown
                    + "; available: " + byId.keySet());
        }
        this.activeChannels = enabledChannelIds.stream().distinct().map(byId::get).toList();
    }

    /** @return how many channels delivered something new (0 when every channel had already delivered this occurrence) */
    public int dispatch(NotificationMessage message) {
        int delivered = 0;
        for (NotificationChannel channel : activeChannels) {
            try {
                if (channel.deliver(message)) {
                    delivered++;
                }
            } catch (RuntimeException ex) {
                log.warn("Notification channel '{}' failed to deliver {} for user {}: {}",
                        channel.id(), message.type(), message.userId(), ex.getMessage());
            }
        }
        return delivered;
    }

    /** The ids of the strategies currently in use, in dispatch order. */
    public List<String> activeChannelIds() {
        return activeChannels.stream().map(NotificationChannel::id).toList();
    }
}
