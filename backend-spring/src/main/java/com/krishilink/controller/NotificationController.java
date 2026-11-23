package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.Notification;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.NotificationRepository;
import com.krishilink.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Notification> list = notificationRepository.findByUserOrderByCreatedAtDesc(principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PatchMapping("/{id}/read")
    @Transactional
    public ResponseEntity<ApiResponse<Notification>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUser().getId().equals(principal.getUser().getId())) {
            throw new ResourceNotFoundException("Notification not found");
        }
        notification.setRead(true);
        notification = notificationRepository.save(notification);
        return ResponseEntity.ok(ApiResponse.ok(notification));
    }

    @PatchMapping("/read-all")
    @Transactional
    public ResponseEntity<ApiResponse<String>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationRepository.markAllAsRead(principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read"));
    }
}
