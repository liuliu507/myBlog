package com.liuliu.example.myblogbackend.controller;

import com.liuliu.example.myblogbackend.common.Result;
import com.liuliu.example.myblogbackend.dto.CommentRequest;
import com.liuliu.example.myblogbackend.dto.CommentVO;
import com.liuliu.example.myblogbackend.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /** 查文章评论树（公开） */
    @GetMapping
    public Result<List<CommentVO>> list(@RequestParam Long articleId) {
        return Result.success(commentService.listByArticle(articleId));
    }

    /** 发表评论/回复（登录） */
    @PostMapping
    public Result<CommentVO> create(@RequestAttribute("userId") Long userId,
                                    @Valid @RequestBody CommentRequest req) {
        return Result.success(commentService.create(userId, req));
    }

    /** 删除评论（评论者本人或文章作者） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestAttribute("userId") Long userId,
                               @PathVariable Long id) {
        commentService.delete(userId, id);
        return Result.success();
    }
}
