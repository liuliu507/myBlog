package com.liuliu.example.myblogbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liuliu.example.myblogbackend.common.Result;
import com.liuliu.example.myblogbackend.dto.NotificationVO;
import com.liuliu.example.myblogbackend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /** 我的通知列表（登录，分页倒序） */
    @GetMapping
    public Result<Page<NotificationVO>> list(@RequestAttribute("userId") Long userId,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return Result.success(notificationService.listMy(userId, page, size));
    }

    /** 未读通知数（铃铛徽标轮询用） */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount(@RequestAttribute("userId") Long userId) {
        return Result.success(Map.of("count", notificationService.unreadCount(userId)));
    }

    /** 全部标记已读 */
    @PostMapping("/read-all")
    public Result<Void> markAllRead(@RequestAttribute("userId") Long userId) {
        notificationService.markAllRead(userId);
        return Result.success();
    }
}
