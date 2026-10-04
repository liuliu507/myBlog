package com.liuliu.example.myblogbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liuliu.example.myblogbackend.common.Result;
import com.liuliu.example.myblogbackend.dto.ArticleRequest;
import com.liuliu.example.myblogbackend.dto.ArticleVO;
import com.liuliu.example.myblogbackend.service.ArticleService;
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
