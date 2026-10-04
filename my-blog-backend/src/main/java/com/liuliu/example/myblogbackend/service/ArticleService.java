package com.liuliu.example.myblogbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liuliu.example.myblogbackend.common.ErrorCode;
import com.liuliu.example.myblogbackend.dto.ArticleRequest;
import com.liuliu.example.myblogbackend.dto.ArticleVO;
import com.liuliu.example.myblogbackend.entity.Article;
import com.liuliu.example.myblogbackend.entity.Category;
import com.liuliu.example.myblogbackend.entity.User;
import com.liuliu.example.myblogbackend.exception.BusinessException;
import com.liuliu.example.myblogbackend.mapper.ArticleMapper;
import com.liuliu.example.myblogbackend.mapper.CategoryMapper;
import com.liuliu.example.myblogbackend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    /** 分页查询已发布文章（公开） */
    public Page<ArticleVO> listPublished(int page, int size) {
        Page<Article> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getCreatedAt);

        Page<Article> result = articleMapper.selectPage(pageParam, wrapper);

        Page<ArticleVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    /** 文章详情（公开，只能看已发布的；作者本人可看自己的草稿） */
    public ArticleVO detail(Long id, Long currentUserId) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }
        // 未发布且不是作者本人，不给看
        if (article.getStatus() == 0 && !article.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }
        return toVO(article);
    }

    /** 我的文章列表（含草稿），可按分类筛选 */
    public List<ArticleVO> myArticles(Long userId, Long categoryId) {
        List<Article> list = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getUserId, userId)
                        .eq(categoryId != null, Article::getCategoryId, categoryId)
                        .orderByDesc(Article::getCreatedAt)
        );
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    /** 新建 */
    public ArticleVO create(Long userId, ArticleRequest req) {
        validateCategoryOwnership(req.getCategoryId(), userId);
        Article a = new Article();
        a.setUserId(userId);
        a.setTitle(req.getTitle());
        a.setSummary(req.getSummary());
        a.setContent(req.getContent());
        a.setCategoryId(req.getCategoryId());
        a.setStatus(req.getStatus() == null ? 0 : req.getStatus());
        articleMapper.insert(a);
        return toVO(a);
    }

    /** 修改，仅作者 */
    public ArticleVO update(Long userId, Long id, ArticleRequest req) {
        Article a = articleMapper.selectById(id);
        if (a == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }
        if (!a.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此文章");
        }
        validateCategoryOwnership(req.getCategoryId(), userId);
        a.setTitle(req.getTitle());
        a.setSummary(req.getSummary());
        a.setContent(req.getContent());
        a.setCategoryId(req.getCategoryId());
        if (req.getStatus() != null) {
            a.setStatus(req.getStatus());
        }

        a.setUpdatedAt(null);
        articleMapper.updateById(a);
        return toVO(a);
    }

    /** 删除，仅作者 */
    public void delete(Long userId, Long id) {
        Article a = articleMapper.selectById(id);
        if (a == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }
        if (!a.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此文章");
        }
        articleMapper.deleteById(id);
    }

    /** 实体列表转 VO 列表（供其他 Service 复用，避免重复映射逻辑） */
    public List<ArticleVO> toVOList(List<Article> articles) {
        return articles.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 水平越权防护：分类为空（未分类）直接放行；
     * 非空时必须存在且归属当前用户，否则一律拒绝（fail-closed）
     */
    private void validateCategoryOwnership(Long categoryId, Long userId) {
        if (categoryId == null) {
            return;
        }
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || !category.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
    }

    /** 实体转 VO，顺便查作者昵称 */
    private ArticleVO toVO(Article a) {
        ArticleVO vo = new ArticleVO();
        vo.setId(a.getId());
        vo.setUserId(a.getUserId());
        vo.setCategoryId(a.getCategoryId());
        vo.setTitle(a.getTitle());
        vo.setSummary(a.getSummary());
        vo.setContent(a.getContent());
        vo.setStatus(a.getStatus());
        vo.setCreatedAt(a.getCreatedAt());
        vo.setUpdatedAt(a.getUpdatedAt());

        User author = userMapper.selectById(a.getUserId());
        if (author != null) {
            vo.setAuthorNickname(author.getNickname());
        }
        if (a.getCategoryId() != null) {
            Category cat = categoryMapper.selectById(a.getCategoryId());
            if (cat != null) {
                vo.setCategoryName(cat.getName());
            }
        }
        return vo;
    }
}