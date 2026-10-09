package com.liuliu.example.myblogbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuliu.example.myblogbackend.dto.ChatMessageVO;
import com.liuliu.example.myblogbackend.entity.ChatMessage;
import com.liuliu.example.myblogbackend.mapper.ChatMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ChatMessageService {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    /** 落库一条对话消息 */
    public void save(Long userId, String role, String content, Long articleId) {
        ChatMessage m = new ChatMessage();
        m.setUserId(userId);
        m.setRole(role);
        m.setContent(content);
        m.setArticleId(articleId);
        chatMessageMapper.insert(m);
    }

    /**
     * 取某用户最近的对话记录：先按时间倒序取 limit 条，再反转为正序，
     * 这样前端拿到后可直接按时间顺序展示。
     */
    public List<ChatMessageVO> listRecent(Long userId, int limit) {
        List<ChatMessage> msgs = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getUserId, userId)
                        .orderByDesc(ChatMessage::getCreatedAt)
                        .last("LIMIT " + limit));
        Collections.reverse(msgs);
        return msgs.stream().map(ChatMessageVO::from).toList();
    }
}
