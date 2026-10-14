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
}
