package com.liuliu.example.myblogbackend.dto;

import lombok.Data;

import java.util.List;

/**
 * 首页分页列表的缓存包装结构。
 * 不直接缓存 MyBatis-Plus 的 Page 对象：Page 内部结构复杂（含 orders、optimizeCountSql 等字段），
 * JSON 反序列化容易踩坑，这里只提取 total + records 两个有效字段，重建 Page 时状态可控
 */
@Data
public class ArticlePageCache {
    private Long total;
    private List<ArticleVO> records;
}
