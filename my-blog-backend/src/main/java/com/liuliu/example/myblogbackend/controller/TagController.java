package com.liuliu.example.myblogbackend.controller;

import com.liuliu.example.myblogbackend.common.Result;
import com.liuliu.example.myblogbackend.dto.TagVO;
import com.liuliu.example.myblogbackend.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    @Autowired
    private TagService tagService;

    /** 标签云（公开）：所有标签及其已发布文章数，按数量倒序 */
    @GetMapping
    public Result<List<TagVO>> tagCloud() {
        return Result.success(tagService.tagCloud());
    }
}
