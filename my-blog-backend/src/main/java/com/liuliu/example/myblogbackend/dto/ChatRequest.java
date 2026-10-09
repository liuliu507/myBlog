package com.liuliu.example.myblogbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "消息不能为空")
    @Size(max = 2000, message = "单条消息最多 2000 字")
    private String message;

    // 可选：文章阅读上下文（由前端在文章页注入，让 AI 能“读懂”当前文章）
    private String articleTitle;

    @Size(max = 8000, message = "文章上下文过长")
    private String articleContent;

    // 可选：文章页对话时关联的文章ID，用于落库
    private Long articleId;
}
