package com.liuliu.example.myblogbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liuliu.example.myblogbackend.dto.NotificationVO;
import com.liuliu.example.myblogbackend.entity.Notification;
import com.liuliu.example.myblogbackend.entity.User;
import com.liuliu.example.myblogbackend.mapper.NotificationMapper;
import com.liuliu.example.myblogbackend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final String TYPE_COMMENT = "comment";
    private static final String TYPE_REPLY = "reply";
    private static final String TYPE_LIKE = "like";

    /** 评论快照截断长度，与表列宽一致 */
    private static final int SNAPSHOT_MAX_LEN = 200;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 评论/回复通知：一级评论（type=comment）通知文章作者，
     * 二级回复（type=reply）通知被回复人——两者文案不同，分开建类型。
     * 自己触发自己（评自己的文章、回复自己）不发通知。
     */
    public void sendComment(boolean isReply, Long receiverId, Long senderId, Long articleId,
                            String articleTitle, String content) {
        if (receiverId == null || receiverId.equals(senderId)) {
            return;
        }
        insert(receiverId, senderId, isReply ? TYPE_REPLY : TYPE_COMMENT,
                articleId, articleTitle, truncate(content));
    }

    /**
     * 点赞通知：同一人对同一篇文章已存在未读通知时不重复发，
     * 防止"点赞→取消→点赞"反复触发刷屏。
     */
    public void sendLike(Long receiverId, Long senderId, Long articleId, String articleTitle) {
        if (receiverId == null || receiverId.equals(senderId)) {
            return;
        }
        boolean unreadExists = notificationMapper.exists(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, receiverId)
                        .eq(Notification::getSenderId, senderId)
                        .eq(Notification::getArticleId, articleId)
                        .eq(Notification::getType, TYPE_LIKE)
                        .eq(Notification::getIsRead, 0));
        if (unreadExists) {
            return;
        }
        insert(receiverId, senderId, TYPE_LIKE, articleId, articleTitle, null);
    }

    /** 我的通知列表（分页，按时间倒序），发送者昵称/头像实时 JOIN user */
    public Page<NotificationVO> listMy(Long userId, int page, int size) {
        Page<Notification> p = notificationMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getId));

        Page<NotificationVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        List<Notification> records = p.getRecords();
        if (records.isEmpty()) {
            result.setRecords(Collections.emptyList());
            return result;
        }

        // 批量查发送者信息，避免 N+1
        List<Long> senderIds = records.stream()
                .map(Notification::getSenderId).distinct().collect(Collectors.toList());
        Map<Long, User> senderMap = senderIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(senderIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        result.setRecords(records.stream()
                .map(n -> toVO(n, senderMap.get(n.getSenderId())))
                .collect(Collectors.toList()));
        return result;
    }

    /** 未读通知数（铃铛徽标轮询用），走 (user_id, is_read, id) 索引 */
    public Long unreadCount(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
    }

    /** 全部标记已读：只 update 命中行，未读为 0 时不会产生写放大 */
    public void markAllRead(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
    }

    private void insert(Long receiverId, Long senderId, String type,
                        Long articleId, String articleTitle, String commentText) {
        Notification n = new Notification();
        n.setUserId(receiverId);
        n.setSenderId(senderId);
        n.setType(type);
        n.setArticleId(articleId);
        n.setArticleTitle(articleTitle);
        n.setCommentText(commentText);
        n.setIsRead(0);
        notificationMapper.insert(n);
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        // 压缩换行/空白，通知列表里只展示一行摘要
        String flat = s.replaceAll("\\s+", " ").trim();
        return flat.length() > SNAPSHOT_MAX_LEN ? flat.substring(0, SNAPSHOT_MAX_LEN) : flat;
    }

    private NotificationVO toVO(Notification n, User sender) {
        NotificationVO vo = new NotificationVO();
        vo.setId(n.getId());
        vo.setType(n.getType());
        vo.setSenderId(n.getSenderId());
        if (sender != null) {
            vo.setSenderNickname(sender.getNickname());
            vo.setSenderAvatar(sender.getAvatar());
        }
        vo.setArticleId(n.getArticleId());
        vo.setArticleTitle(n.getArticleTitle());
        vo.setCommentText(n.getCommentText());
        vo.setIsRead(n.getIsRead() != null && n.getIsRead() == 1);
        vo.setCreatedAt(n.getCreatedAt());
        return vo;
    }
}
