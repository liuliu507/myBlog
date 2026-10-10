package com.liuliu.example.myblogbackend.dto;

import lombok.Data;

/** 标签云元素：标签 + 已发布文章数 */
@Data
public class TagVO {
    private Long id;
    private String name;
    private Long articleCount;
}
