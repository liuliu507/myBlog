package com.liuliu.example.myblogbackend.controller;

import com.liuliu.example.myblogbackend.common.Result;
import com.liuliu.example.myblogbackend.dto.CategoryDetailVO;
import com.liuliu.example.myblogbackend.dto.CategoryRequest;
import com.liuliu.example.myblogbackend.dto.CategoryVO;
import com.liuliu.example.myblogbackend.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public Result<List<CategoryVO>> list(@RequestAttribute("userId") Long userId) {
        return Result.success(categoryService.myList(userId));
    }

    @GetMapping("/{id}")
    public Result<CategoryDetailVO> detail(@PathVariable Long id) {
        return Result.success(categoryService.publicDetail(id));
    }

    @PostMapping
    public Result<CategoryVO> create(@RequestAttribute("userId") Long userId,
                                     @Valid @RequestBody CategoryRequest req) {
        return Result.success(categoryService.create(userId, req));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@RequestAttribute("userId") Long userId,
                               @PathVariable Long id,
                               @Valid @RequestBody CategoryRequest req) {
        categoryService.update(userId, id, req);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestAttribute("userId") Long userId,
                               @PathVariable Long id) {
        categoryService.delete(userId, id);
        return Result.success();
    }
}
