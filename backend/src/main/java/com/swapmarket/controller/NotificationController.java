package com.swapmarket.controller;

import com.swapmarket.common.Result;
import com.swapmarket.entity.Notification;
import com.swapmarket.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/list")
    public Result<List<Notification>> listNotifications(@RequestParam(defaultValue = "1") Long userId) {
        return Result.success(notificationService.getNotifications(userId));
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestParam(defaultValue = "1") Long userId) {
        return Result.success(notificationService.getUnreadCount(userId));
    }

    @PostMapping("/read/{id}")
    public Result<Void> markAsRead(@RequestParam(defaultValue = "1") Long userId, @PathVariable Long id) {
        notificationService.markAsRead(userId, id);
        return Result.success();
    }

    @PostMapping("/read-all")
    public Result<Void> markAllAsRead(@RequestParam(defaultValue = "1") Long userId) {
        notificationService.markAllAsRead(userId);
        return Result.success();
    }
}
