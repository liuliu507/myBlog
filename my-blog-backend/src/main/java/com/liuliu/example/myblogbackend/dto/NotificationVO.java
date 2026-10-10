package com.liuliu.example.myblogbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 站内通知列表项 */
@Data
public class NotificationVO {
    private Long id;
    /** comment / like */
    private String type;
    private Long senderId;
    /** 发送者昵称/头像实时 JOIN user 表，保证改名换头像后展示最新 */
    private String senderNickname;
    private String senderAvatar;
    private Long articleId;
    private String articleTitle;
    private String commentText;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
