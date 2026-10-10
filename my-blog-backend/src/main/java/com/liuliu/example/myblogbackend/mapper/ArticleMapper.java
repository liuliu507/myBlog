package com.liuliu.example.myblogbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liuliu.example.myblogbackend.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 全文搜索（ngram 分词，布尔模式），按相关度打分倒序。
     * 分页插件会自动追加 LIMIT 并生成 count 查询。
     */
    @Select("SELECT * FROM article WHERE status = 1 "
            + "AND MATCH(title, summary, content) AGAINST (#{keyword} IN BOOLEAN MODE) "
            + "ORDER BY MATCH(title, summary, content) AGAINST (#{keyword} IN BOOLEAN MODE) DESC, created_at DESC")
    Page<Article> searchFulltext(Page<Article> page, @Param("keyword") String keyword);

    /**
     * 单字关键词的降级方案：ngram 默认二元切词，单个汉字匹配不到全文索引，改用 LIKE。
     * 前导通配符无法走索引，但单字搜索频率低、可接受；英文单词搜索仍走全文索引
     */
    @Select("SELECT * FROM article WHERE status = 1 "
            + "AND (title LIKE CONCAT('%', #{keyword}, '%') "
            + "OR summary LIKE CONCAT('%', #{keyword}, '%') "
            + "OR content LIKE CONCAT('%', #{keyword}, '%')) "
            + "ORDER BY created_at DESC")
    Page<Article> searchLike(Page<Article> page, @Param("keyword") String keyword);
}
