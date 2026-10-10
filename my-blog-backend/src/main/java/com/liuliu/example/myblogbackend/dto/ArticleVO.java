package com.liuliu.example.myblogbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleVO {
    private Long id;
    private Long userId;
    private String authorNickname;   // 作者昵称，列表展示用
    private String authorAvatar;     // 作者头像 URL，列表展示用
    private Long categoryId;
    private String title;
    private String summary;
    private String content;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String categoryName;
}
