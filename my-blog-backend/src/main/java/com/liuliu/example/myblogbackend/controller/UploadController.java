package com.liuliu.example.myblogbackend.controller;

import com.liuliu.example.myblogbackend.common.Result;
import com.liuliu.example.myblogbackend.service.CosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 图片上传接口（走 JWT 拦截器，userId 从 @RequestAttribute 取）
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private CosService cosService;

    /** 上传头像 → 返回 COS 公网 URL */
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(
            @RequestAttribute("userId") Long userId,
            @RequestParam("file") MultipartFile file) {
        String url = cosService.uploadImage(file, "avatar", userId);
        return Result.success(Map.of("url", url));
    }

    /** 上传文章内嵌图片 → 返回 COS 公网 URL，前端插入 Markdown */
    @PostMapping("/article-image")
    public Result<Map<String, String>> uploadArticleImage(
            @RequestAttribute("userId") Long userId,
            @RequestParam("file") MultipartFile file) {
        String url = cosService.uploadImage(file, "article", userId);
        return Result.success(Map.of("url", url));
    }
}
