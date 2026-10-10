package com.liuliu.example.myblogbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 文章内的标签引用：只暴露 id（跳转用）和 name（展示用） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagRef {
    private Long id;
    private String name;
}
