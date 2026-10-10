package com.liuliu.example.myblogbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuliu.example.myblogbackend.common.ErrorCode;
import com.liuliu.example.myblogbackend.dto.CommentRequest;
import com.liuliu.example.myblogbackend.dto.CommentVO;
import com.liuliu.example.myblogbackend.entity.Article;
import com.liuliu.example.myblogbackend.entity.Comment;
import com.liuliu.example.myblogbackend.entity.User;
import com.liuliu.example.myblogbackend.exception.BusinessException;
import com.liuliu.example.myblogbackend.mapper.ArticleMapper;
import com.liuliu.example.myblogbackend.mapper.CommentMapper;
import com.liuliu.example.myblogbackend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    /** 发表评论（登录）；parentId 非空表示回复某条一级评论（只允许二级） */
    public CommentVO create(Long userId, CommentRequest req) {
        Article article = articleMapper.selectById(req.getArticleId());
        if (article == null || article.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }

        Comment parent = null;
        if (req.getParentId() != null) {
            parent = commentMapper.selectById(req.getParentId());
            // 回复必须指向同一文章下的一级评论，防止串楼/无限嵌套
            if (parent == null
                    || !parent.getArticleId().equals(req.getArticleId())
                    || parent.getParentId() != null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "回复的评论不存在");
            }
        }

        Comment c = new Comment();
        c.setArticleId(req.getArticleId());
        c.setUserId(userId);
        c.setParentId(req.getParentId());
        c.setContent(req.getContent());
        commentMapper.insert(c);
        return toVO(c, loadUsers(List.of(userId)));
    }

    /**
     * 查询文章评论树（公开）：一级评论按时间正序，每条带二级回复列表
     */
    public List<CommentVO> listByArticle(Long articleId) {
        List<Comment> all = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getArticleId, articleId)
                        .orderByAsc(Comment::getCreatedAt)
        );
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查所有评论人，避免 N+1
        List<Long> userIds = all.stream().map(Comment::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = loadUsers(userIds);

        List<CommentVO> roots = all.stream()
                .filter(c -> c.getParentId() == null)
                .map(c -> toVO(c, userMap))
                .collect(Collectors.toList());

        Map<Long, List<CommentVO>> replyMap = all.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(
                        Comment::getParentId,
                        Collectors.mapping(c -> toVO(c, userMap), Collectors.toList())
                ));

        roots.forEach(r -> r.setReplies(replyMap.getOrDefault(r.getId(), Collections.emptyList())));
        return roots;
    }

    /** 删除评论：评论者本人或文章作者可删；删除一级评论会连带删除其所有回复 */
    public void delete(Long userId, Long commentId) {
        Comment c = commentMapper.selectById(commentId);
        if (c == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }
        Article article = articleMapper.selectById(c.getArticleId());
        boolean isCommentAuthor = c.getUserId().equals(userId);
        boolean isArticleAuthor = article != null && article.getUserId().equals(userId);
        if (!isCommentAuthor && !isArticleAuthor) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此评论");
        }
        if (c.getParentId() == null) {
            // 连带删除二级回复
            commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getParentId, commentId));
        }
        commentMapper.deleteById(commentId);
    }

    private Map<Long, User> loadUsers(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private CommentVO toVO(Comment c, Map<Long, User> userMap) {
        CommentVO vo = new CommentVO();
        vo.setId(c.getId());
        vo.setArticleId(c.getArticleId());
        vo.setUserId(c.getUserId());
        vo.setParentId(c.getParentId());
        vo.setContent(c.getContent());
        vo.setCreatedAt(c.getCreatedAt());
        User u = userMap.get(c.getUserId());
        if (u != null) {
            vo.setNickname(u.getNickname());
            vo.setAvatar(u.getAvatar());
        }
        vo.setReplies(Collections.emptyList());
        return vo;
    }
}
