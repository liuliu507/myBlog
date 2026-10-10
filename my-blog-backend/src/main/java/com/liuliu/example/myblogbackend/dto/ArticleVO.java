package com.liuliu.example.myblogbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    private Integer viewCount;          // 浏览次数
    private Long likeCount;             // 点赞数（实时 count，仅详情接口填充）
    private Boolean likedByMe;          // 当前用户是否已点赞（仅详情接口填充）
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String categoryName;
    private List<TagRef> tags;          // 文章标签（id + 名称）
}
