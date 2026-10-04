package com.liuliu.example.myblogbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人专栏公开详情：任何访客可查看，
 * articles 中只包含该专栏下已发布的文章
 */
@Data
public class CategoryDetailVO {
    private Long id;
    private String name;
    private Long authorId;
    private String authorNickname;
    private LocalDateTime createdAt;
    private List<ArticleVO> articles;
}
