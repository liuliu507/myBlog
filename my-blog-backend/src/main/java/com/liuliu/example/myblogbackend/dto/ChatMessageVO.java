package com.liuliu.example.myblogbackend.dto;

import com.liuliu.example.myblogbackend.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO {
    private Long id;
    private String role;
    private String content;
    private Long articleId;
    private LocalDateTime createdAt;

    public static ChatMessageVO from(ChatMessage m) {
        return new ChatMessageVO(m.getId(), m.getRole(), m.getContent(), m.getArticleId(), m.getCreatedAt());
    }
}
