package com.cohort.api.controller;

import com.cohort.api.dto.MarkAllReadResponse;
import com.cohort.api.dto.NotificationResponse;
import com.cohort.api.dto.PagedResponse;
import com.cohort.api.dto.UnreadCountResponse;
import com.cohort.api.service.notification.NotificationInboxService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationInboxService inboxService;

    public NotificationController(NotificationInboxService inboxService) {
        this.inboxService = inboxService;
    }

    @GetMapping
    public PagedResponse<NotificationResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "all") NotificationInboxService.Filter status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return inboxService.list(userId(jwt), status, page, size);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return inboxService.unreadCount(userId(jwt));
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return inboxService.markRead(userId(jwt), id);
    }

    @PostMapping("/read-all")
    public MarkAllReadResponse markAllRead(@AuthenticationPrincipal Jwt jwt) {
        return inboxService.markAllRead(userId(jwt));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
