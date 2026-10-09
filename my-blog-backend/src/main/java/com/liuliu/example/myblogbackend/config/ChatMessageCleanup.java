package com.liuliu.example.myblogbackend.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuliu.example.myblogbackend.entity.ChatMessage;
import com.liuliu.example.myblogbackend.mapper.ChatMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ChatMessageCleanup {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    // 每天凌晨 3 点清理 3 天前的 AI 对话记录，防止表无限膨胀
    @Scheduled(cron = "0 0 3 * * ?")
    public void clean() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(3);
        int deleted = chatMessageMapper.delete(
                new LambdaQueryWrapper<ChatMessage>()
                        .lt(ChatMessage::getCreatedAt, cutoff));
        log.info("清理3天前AI对话记录，删除 {} 条（截止 {}）", deleted, cutoff);
    }
}
