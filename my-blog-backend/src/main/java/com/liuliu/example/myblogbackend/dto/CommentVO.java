package com.liuliu.example.myblogbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {
    private Long id;
    private Long articleId;
    private Long userId;
    private Long parentId;
    private String content;
    private LocalDateTime createdAt;

    /** 评论人信息（用于展示） */
    private String nickname;
    private String avatar;

    /** 二级回复列表（一级评论的 replies） */
    private List<CommentVO> replies;
}
