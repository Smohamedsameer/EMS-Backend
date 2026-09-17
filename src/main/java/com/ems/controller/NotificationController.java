package com.ems.controller;

import com.ems.dto.NotificationResponse;
import com.ems.security.CurrentUser;
import com.ems.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> myNotifications() {
        return ResponseEntity.ok(notificationService.getMyNotifications(CurrentUser.userId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount(CurrentUser.userId())));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        notificationService.markAsRead(id, CurrentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
