package com.liuliu.example.myblogbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryVO {
    private Long id;
    private String name;
    private Integer articleCount;
    private LocalDateTime createdAt;
}
