package com.liuliu.example.myblogbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuliu.example.myblogbackend.common.ErrorCode;
import com.liuliu.example.myblogbackend.dto.CategoryDetailVO;
import com.liuliu.example.myblogbackend.dto.CategoryRequest;
import com.liuliu.example.myblogbackend.dto.CategoryVO;
import com.liuliu.example.myblogbackend.entity.Article;
import com.liuliu.example.myblogbackend.entity.Category;
import com.liuliu.example.myblogbackend.entity.User;
import com.liuliu.example.myblogbackend.exception.BusinessException;
import com.liuliu.example.myblogbackend.mapper.ArticleMapper;
import com.liuliu.example.myblogbackend.mapper.CategoryMapper;
import com.liuliu.example.myblogbackend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ArticleService articleService;

    /** 专栏公开详情（任何访客可看，只含该专栏下已发布文章） */
    public CategoryDetailVO publicDetail(Long id) {
        Category c = categoryMapper.selectById(id);
        if (c == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }

        List<Article> published = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getCategoryId, id)
                        .eq(Article::getStatus, 1)
                        .orderByDesc(Article::getCreatedAt)
        );

        User author = userMapper.selectById(c.getUserId());

        CategoryDetailVO vo = new CategoryDetailVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setAuthorId(c.getUserId());
        vo.setCreatedAt(c.getCreatedAt());
        if (author != null) {
            vo.setAuthorNickname(author.getNickname());
        }
        vo.setArticles(articleService.toVOList(published));
        return vo;
    }

    // 我的分类列表
    public List<CategoryVO> myList(Long userId) {
        List<Category> list = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getUserId, userId)
                        .orderByDesc(Category::getCreatedAt)
        );
        return list.stream().map(c -> {
            CategoryVO vo = new CategoryVO();
            vo.setId(c.getId());
            vo.setName(c.getName());
            vo.setCreatedAt(c.getCreatedAt());
            // 统计该分类下的文章数
            Long count = articleMapper.selectCount(
                    new LambdaQueryWrapper<Article>().eq(Article::getCategoryId, c.getId())
            );
            vo.setArticleCount(count == null ? 0 : count.intValue());
            return vo;
        }).collect(Collectors.toList());
    }

    // 新建
    public CategoryVO create(Long userId, CategoryRequest req) {
        Long count = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getUserId, userId)
                        .eq(Category::getName, req.getName())
        );
        if (count!=null && count>0){
            throw new BusinessException(ErrorCode.CONFLICT, "分类名已存在");
        }

        Category c = new Category();
        c.setUserId(userId);
        c.setName(req.getName());
        categoryMapper.insert(c);

        CategoryVO vo = new CategoryVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setArticleCount(0);
        vo.setCreatedAt(c.getCreatedAt());
        return vo;
    }

    //重命名
    public void update(Long userId, Long id, CategoryRequest req){
        Category c = categoryMapper.selectById(id);
        if (c==null){
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        if (!c.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此分类");
        }
        Long count = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getUserId, userId)
                        .eq(Category::getName, req.getName())
                        .ne(Category::getId, id)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "分类名已存在");
        }
        c.setName(req.getName());
        categoryMapper.updateById(c);
    }

    // 删除
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        Category c = categoryMapper.selectById(id);
        if (c == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        if (!c.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此分类");
        }
        articleMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Article>()
                        .eq(Article::getCategoryId, id)
                        .set(Article::getCategoryId, null)
        );
        categoryMapper.deleteById(id);
    }

}
