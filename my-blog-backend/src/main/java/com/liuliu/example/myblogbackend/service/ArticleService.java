package com.liuliu.example.myblogbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liuliu.example.myblogbackend.common.ErrorCode;
import com.liuliu.example.myblogbackend.dto.ArticlePageCache;
import com.liuliu.example.myblogbackend.dto.ArticleRequest;
import com.liuliu.example.myblogbackend.dto.ArticleVO;
import com.liuliu.example.myblogbackend.entity.Article;
import com.liuliu.example.myblogbackend.entity.ArticleLike;
import com.liuliu.example.myblogbackend.entity.Category;
import com.liuliu.example.myblogbackend.entity.User;
import com.liuliu.example.myblogbackend.exception.BusinessException;
import com.liuliu.example.myblogbackend.mapper.ArticleLikeMapper;
import com.liuliu.example.myblogbackend.mapper.ArticleMapper;
import com.liuliu.example.myblogbackend.mapper.CategoryMapper;
import com.liuliu.example.myblogbackend.mapper.UserMapper;
import com.liuliu.example.myblogbackend.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ArticleService {

    /** 首页分页缓存 key 前缀：blog:article:list:{page}:{size} */
    private static final String LIST_KEY_PREFIX = "blog:article:list:";
    /** 文章详情缓存 key 前缀：blog:article:detail:{id} */
    private static final String DETAIL_KEY_PREFIX = "blog:article:detail:";
    /** 缓存基础 TTL（秒），写入时再加随机偏移防雪崩 */
    private static final long CACHE_TTL_SECONDS = 30 * 60;
    /** TTL 随机抖动上限（秒） */
    private static final long CACHE_TTL_JITTER_SECONDS = 5 * 60;
    /** 不存在文章的空值缓存 TTL（秒）：防穿透 */
    private static final long EMPTY_TTL_SECONDS = 60;
    /** 缓存重建互斥锁的过期时间（秒） */
    private static final long LOCK_TTL_SECONDS = 10;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ObjectMapper objectMapper;

    /** 分页查询已发布文章（公开）。Cache Aside：读时未命中回填，写时删除 */
    public Page<ArticleVO> listPublished(int page, int size) {
        String cacheKey = LIST_KEY_PREFIX + page + ":" + size;

        // 1. 先查缓存
        String cached = redisUtil.get(cacheKey);
        if (cached != null) {
            try {
                ArticlePageCache cp = objectMapper.readValue(cached, ArticlePageCache.class);
                Page<ArticleVO> voPage = new Page<>(page, size);
                voPage.setTotal(cp.getTotal());
                voPage.setRecords(cp.getRecords());
                return voPage;
            } catch (Exception e) {
                // 缓存数据损坏：记日志后降级查库并重建缓存
                log.warn("列表缓存反序列化失败，降级查库: key={}", cacheKey, e);
            }
        }

        // 2. 未命中查库
        Page<Article> result = articleMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, 1)
                        .orderByDesc(Article::getCreatedAt));

        Page<ArticleVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));

        // 3. 回填缓存，TTL 加随机偏移防雪崩（大量 key 同时过期会瞬间冲垮数据库）
        try {
            ArticlePageCache cp = new ArticlePageCache();
            cp.setTotal(voPage.getTotal());
            cp.setRecords(voPage.getRecords());
            long ttl = CACHE_TTL_SECONDS + ThreadLocalRandom.current().nextLong(0, CACHE_TTL_JITTER_SECONDS);
            redisUtil.set(cacheKey, objectMapper.writeValueAsString(cp), ttl);
        } catch (Exception e) {
            log.warn("列表缓存写入失败: key={}", cacheKey, e);
        }
        return voPage;
    }

    /**
     * 文章详情（公开，只能看已发布的；作者本人可看自己的草稿）。
     * 缓存策略：
     * - 只缓存已发布文章的主体（草稿涉及权限，缓存会造成未授权读取，直接走库）
     * - 空值缓存 60s 防穿透：不存在的 id 反复查库会被拦下
     * - 互斥锁防击穿：热点 key 过期瞬间只放一个线程去重建，其他线程等待重试
     * - 点赞状态/点赞数/浏览数不进缓存：前两者与"谁在看"或实时性相关，浏览数高频变化，返回时单独查
     */
    public ArticleVO detail(Long id, Long currentUserId) {
        String cacheKey = DETAIL_KEY_PREFIX + id;
        String lockKey = DETAIL_KEY_PREFIX + id + ":lock";

        // 1. 先查缓存；空串是穿透防护写入的空值标记
        String cached = redisUtil.get(cacheKey);
        if ("".equals(cached)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }
        if (cached != null) {
            return finishDetail(deserializeDetail(cached, id, currentUserId), id, currentUserId);
        }

        // 2. 未命中：抢互斥锁，抢到的线程负责查库重建缓存
        if (redisUtil.setIfAbsent(lockKey, "1", LOCK_TTL_SECONDS)) {
            try {
                // 双重检查：拿锁前可能已有线程完成回填
                cached = redisUtil.get(cacheKey);
                if ("".equals(cached)) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
                }
                if (cached != null) {
                    return finishDetail(deserializeDetail(cached, id, currentUserId), id, currentUserId);
                }
                return finishDetail(loadFromDb(id, currentUserId, true), id, currentUserId);
            } finally {
                redisUtil.delete(lockKey);
            }
        }

        // 3. 没抢到锁：稍等重试读缓存，仍无则降级直接查库（不回填，避免锁外并发写）
        try {
            Thread.sleep(80);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        cached = redisUtil.get(cacheKey);
        if ("".equals(cached)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }
        if (cached != null) {
            return finishDetail(deserializeDetail(cached, id, currentUserId), id, currentUserId);
        }
        return finishDetail(loadFromDb(id, currentUserId, false), id, currentUserId);
    }

    /** 查库加载文章 VO；writeCache=true 时回填缓存（仅已发布文章），查不到写空值标记 */
    private ArticleVO loadFromDb(Long id, Long currentUserId, boolean writeCache) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            if (writeCache) {
                redisUtil.set(DETAIL_KEY_PREFIX + id, "", EMPTY_TTL_SECONDS);
            }
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }
        // 未发布且不是作者本人，不给看
        if (article.getStatus() == 0 && !article.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }
        ArticleVO vo = toVO(article);
        // 草稿涉及权限隔离，不缓存；已发布才回填
        if (writeCache && article.getStatus() == 1) {
            try {
                long ttl = CACHE_TTL_SECONDS + ThreadLocalRandom.current().nextLong(0, CACHE_TTL_JITTER_SECONDS);
                redisUtil.set(DETAIL_KEY_PREFIX + id, objectMapper.writeValueAsString(vo), ttl);
            } catch (Exception e) {
                log.warn("详情缓存写入失败: id={}", id, e);
            }
        }
        return vo;
    }

    /** 缓存 JSON 反序列化；失败（如结构变更/损坏）时降级查库并重建缓存 */
    private ArticleVO deserializeDetail(String json, Long id, Long currentUserId) {
        try {
            return objectMapper.readValue(json, ArticleVO.class);
        } catch (Exception e) {
            log.warn("详情缓存反序列化失败，降级查库: id={}", id, e);
            redisUtil.delete(DETAIL_KEY_PREFIX + id);
            return loadFromDb(id, currentUserId, true);
        }
    }

    /**
     * 详情实时字段填充（缓存命中后也要覆盖，保证不返回过期数据）：
     * - 点赞数：设计上选了实时 count
     * - 是否点赞：个性化数据，缓存会导致 A 用户看到 B 的状态
     * - 浏览数：每次浏览都在变，单独查一行（只取 view_count 列，代价极小）
     */
    private ArticleVO finishDetail(ArticleVO vo, Long id, Long currentUserId) {
        Article fresh = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .select(Article::getViewCount)
                .eq(Article::getId, id));
        vo.setViewCount(fresh != null ? fresh.getViewCount() : 0);
        // 点赞信息：实时 count（数据量小无需冗余字段，日后成瓶颈再加缓存）
        vo.setLikeCount(articleLikeMapper.selectCount(
                new LambdaQueryWrapper<ArticleLike>().eq(ArticleLike::getArticleId, id)));
        vo.setLikedByMe(currentUserId != null && articleLikeMapper.exists(
                new LambdaQueryWrapper<ArticleLike>()
                        .eq(ArticleLike::getArticleId, id)
                        .eq(ArticleLike::getUserId, currentUserId)));
        return vo;
    }

    /**
     * 点赞/取消点赞切换（登录）。
     * 依赖 article_like 的 (article_id, user_id) 唯一索引兜底，并发下重复插入会失败。
     */
    public ArticleVO toggleLike(Long articleId, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }
        LambdaQueryWrapper<ArticleLike> wrapper = new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getArticleId, articleId)
                .eq(ArticleLike::getUserId, userId);
        if (articleLikeMapper.exists(wrapper)) {
            articleLikeMapper.delete(wrapper);
        } else {
            ArticleLike like = new ArticleLike();
            like.setArticleId(articleId);
            like.setUserId(userId);
            articleLikeMapper.insert(like);
        }
        ArticleVO vo = new ArticleVO();
        vo.setId(articleId);
        vo.setLikeCount(articleLikeMapper.selectCount(
                new LambdaQueryWrapper<ArticleLike>().eq(ArticleLike::getArticleId, articleId)));
        vo.setLikedByMe(articleLikeMapper.exists(wrapper));
        return vo;
    }

    /**
     * 记录浏览：同一用户（未登录用 IP）24h 内只计 1 次（Redis SET NX EX 原子防刷）。
     * 命中防刷窗口时静默忽略，仍返回当前浏览数。
     */
    public Integer recordView(Long articleId, Long userId, String ip) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }
        String viewer = userId != null ? "u:" + userId : "ip:" + ip;
        boolean firstViewToday = redisUtil.setIfAbsent("view:" + articleId + ":" + viewer, "1", 86400);
        if (firstViewToday) {
            // 用 SQL 自增而不是查改回写，避免并发丢失更新
            articleMapper.update(null, new UpdateWrapper<Article>()
                    .eq("id", articleId)
                    .setSql("view_count = view_count + 1"));
            return article.getViewCount() + 1;
        }
        return article.getViewCount();
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
        // Cache Aside 写路径：先更新库，再删缓存（新文章会出现在首页列表，列表缓存需失效）
        evictArticleCache(a.getId());
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
        // 写路径删缓存：内容变了删详情，草稿转发布/下架也会影响列表
        evictArticleCache(id);
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
        evictArticleCache(id);
    }

    /**
     * 写操作后失效缓存（Cache Aside：先更新数据库，再删除缓存，下次读取自然重建）。
     * 删除而不是更新缓存：写多字段没必要整体重写，且并发写时"更新缓存"容易把旧数据写回去
     */
    private void evictArticleCache(Long articleId) {
        if (articleId != null) {
            redisUtil.delete(DETAIL_KEY_PREFIX + articleId);
        }
        // 列表缓存的 key 含分页参数，无法精确列出，按前缀 SCAN 清理
        redisUtil.deleteByPrefix(LIST_KEY_PREFIX);
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
        vo.setViewCount(a.getViewCount());
        vo.setCreatedAt(a.getCreatedAt());
        vo.setUpdatedAt(a.getUpdatedAt());

        User author = userMapper.selectById(a.getUserId());
        if (author != null) {
            vo.setAuthorNickname(author.getNickname());
            vo.setAuthorAvatar(author.getAvatar());
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