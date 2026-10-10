package com.liuliu.example.myblogbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liuliu.example.myblogbackend.common.Result;
import com.liuliu.example.myblogbackend.dto.ArticleRequest;
import com.liuliu.example.myblogbackend.dto.ArticleVO;
import com.liuliu.example.myblogbackend.service.ArticleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @GetMapping
    public Result<Page<ArticleVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(articleService.listPublished(page,size));
    }

    @GetMapping("/{id}")
    public Result<ArticleVO> detail(@PathVariable Long id,
                                    @RequestAttribute(value = "userId",required = false) Long userId) {
        return Result.success(articleService.detail(id,userId));
    }

    /** 点赞/取消点赞切换（登录），返回最新点赞数和状态 */
    @PostMapping("/{id}/like")
    public Result<ArticleVO> toggleLike(@RequestAttribute("userId") Long userId,
                                        @PathVariable Long id) {
        return Result.success(articleService.toggleLike(id, userId));
    }

    /** 记录浏览（公开，登录用户按 userId 防刷，未登录按 IP 防刷，24h 窗口） */
    @PostMapping("/{id}/view")
    public Result<Integer> recordView(@RequestAttribute(value = "userId", required = false) Long userId,
                                      @PathVariable Long id,
                                      HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return Result.success(articleService.recordView(id, userId, ip));
    }

    @GetMapping("/mine")
    public Result<List<ArticleVO>> mine(@RequestAttribute("userId") Long userId,
                                        @RequestParam(required = false) Long categoryId) {
        return Result.success(articleService.myArticles(userId, categoryId));
    }

    @PostMapping
    public Result<ArticleVO> create(@RequestAttribute("userId") Long userId,
                                    @Valid@RequestBody ArticleRequest req) {
        return Result.success(articleService.create(userId,req));
    }

    @PutMapping("/{id}")
    public Result<ArticleVO> update(@RequestAttribute("userId") Long userId,
                                    @PathVariable Long id,
                                    @Valid @RequestBody ArticleRequest req) {
        return Result.success(articleService.update(userId,id,req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestAttribute("userId") Long userId,
                               @PathVariable Long id) {
        articleService.delete(userId, id);
        return Result.success();
    }
}
