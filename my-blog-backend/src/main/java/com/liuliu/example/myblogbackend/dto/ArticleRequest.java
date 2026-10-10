package com.liuliu.example.myblogbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ArticleRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最多 200 个字符")
    private String title;

    @Size(max = 500, message = "摘要最多 500 个字符")
    private String summary;

    @NotBlank(message = "正文不能为空")
    private String content;

    private Long categoryId;

    /** 标签名列表：已存在则复用，不存在则创建（全局共享） */
    private List<String> tags;

    // 0草稿 1发布，不传默认草稿
    private Integer status = 0;
}
