package com.liuliu.example.myblogbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liuliu.example.myblogbackend.common.ErrorCode;
import com.liuliu.example.myblogbackend.dto.ArticlePageCache;
import com.liuliu.example.myblogbackend.dto.ArticleRequest;
import com.liuliu.example.myblogbackend.dto.TagRef;
import com.liuliu.example.myblogbackend.dto.ArticleVO;
import com.liuliu.example.myblogbackend.entity.Article;
import com.liuliu.example.myblogbackend.entity.ArticleLike;
import com.liuliu.example.myblogbackend.entity.ArticleTag;
import com.liuliu.example.myblogbackend.entity.Category;
import com.liuliu.example.myblogbackend.entity.Tag;
import com.liuliu.example.myblogbackend.entity.User;
import com.liuliu.example.myblogbackend.exception.BusinessException;
import com.liuliu.example.myblogbackend.mapper.ArticleLikeMapper;
import com.liuliu.example.myblogbackend.mapper.ArticleMapper;
import com.liuliu.example.myblogbackend.mapper.ArticleTagMapper;
import com.liuliu.example.myblogbackend.mapper.CategoryMapper;
import com.liuliu.example.myblogbackend.mapper.TagMapper;
import com.liuliu.example.myblogbackend.mapper.UserMapper;
import com.liuliu.example.myblogbackend.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    private TagMapper tagMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 分页查询已发布文章（公开），可按标签筛选。Cache Aside：读时未命中回填，写时删除。
     * 搜索接口不走这里：关键词组合高基数、命中率低，不缓存
     */
    public Page<ArticleVO> listPublished(int page, int size, Long tagId) {
        String cacheKey = LIST_KEY_PREFIX + (tagId == null ? 0 : tagId) + ":" + page + ":" + size;

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

        // 2. 未命中查库；标签筛选先经 article_tag 中间表取出文章 id 集合
        Page<Article> result;
        if (tagId == null) {
            result = articleMapper.selectPage(new Page<>(page, size),
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getStatus, 1)
                            .orderByDesc(Article::getCreatedAt));
        } else {
            List<Long> articleIds = articleTagMapper.selectList(
                            new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, tagId))
                    .stream().map(ArticleTag::getArticleId).distinct().collect(Collectors.toList());
            if (articleIds.isEmpty()) {
                // 该标签下没有任何文章：直接返回空页（避免 in() 空集合生成非法 SQL）
                Page<ArticleVO> empty = new Page<>(page, size, 0);
                empty.setRecords(List.of());
                cacheListPage(cacheKey, empty);
                return empty;
            }
            result = articleMapper.selectPage(new Page<>(page, size),
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getStatus, 1)
                            .in(Article::getId, articleIds)
                            .orderByDesc(Article::getCreatedAt));
        }

        Page<ArticleVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        // 批量填充标签，一条 IN 查询解决整页（避免 N+1）
        fillTags(voPage.getRecords());

        // 3. 回填缓存，TTL 加随机偏移防雪崩（大量 key 同时过期会瞬间冲垮数据库）
        cacheListPage(cacheKey, voPage);
        return voPage;
    }

    /** 列表分页写缓存（TTL 随机抖动），失败仅记日志不影响主流程 */
    private void cacheListPage(String cacheKey, Page<ArticleVO> voPage) {
        try {
            ArticlePageCache cp = new ArticlePageCache();
            cp.setTotal(voPage.getTotal());
            cp.setRecords(voPage.getRecords());
            long ttl = CACHE_TTL_SECONDS + ThreadLocalRandom.current().nextLong(0, CACHE_TTL_JITTER_SECONDS);
            redisUtil.set(cacheKey, objectMapper.writeValueAsString(cp), ttl);
        } catch (Exception e) {
            log.warn("列表缓存写入失败: key={}", cacheKey, e);
        }
    }

    /**
     * 全文搜索（公开，不缓存）。
     * 关键词 ≥2 个字符：走 FULLTEXT + ngram 布尔模式，按相关度排序；
     * 单字符：ngram 二元切词匹配不到，降级 LIKE；
     * 布尔模式特殊字符（+ - * " 等）会被清洗，避免注入搜索语法导致报错
     */
    public Page<ArticleVO> search(String rawKeyword, int page, int size) {
        String keyword = rawKeyword == null ? "" : rawKeyword.trim();
        if (keyword.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "搜索关键词不能为空");
        }
        if (keyword.length() > 50) {
            keyword = keyword.substring(0, 50);
        }

        String sanitized = keyword.replaceAll("[+\\-><()~*\"@^]+", " ").trim();
        Page<Article> result;
        if (sanitized.length() >= 2) {
            result = articleMapper.searchFulltext(new Page<>(page, size), sanitized);
        } else {
            // 单字或纯操作符：降级 LIKE
            result = articleMapper.searchLike(new Page<>(page, size), keyword);
        }

        Page<ArticleVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        fillTags(voPage.getRecords());
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
        vo.setTags(getTagRefs(id));
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
        List<ArticleVO> vos = list.stream().map(this::toVO).collect(Collectors.toList());
        fillTags(vos);
        return vos;
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
        // 保存标签关联（全局标签：存在复用，不存在创建）
        syncArticleTags(a.getId(), req.getTags());
        // Cache Aside 写路径：先更新库，再删缓存（新文章会出现在首页列表，列表缓存需失效）
        evictArticleCache(a.getId());
        ArticleVO vo = toVO(a);
        vo.setTags(getTagRefs(a.getId()));
        return vo;
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
        // 整表替换标签关联
        syncArticleTags(id, req.getTags());
        // 写路径删缓存：内容变了删详情，草稿转发布/下架也会影响列表
        evictArticleCache(id);
        ArticleVO vo = toVO(a);
        vo.setTags(getTagRefs(id));
        return vo;
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
        // 清理标签关联（标签本身是全局共享的，不删）
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, id));
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
        List<ArticleVO> vos = articles.stream().map(this::toVO).collect(Collectors.toList());
        fillTags(vos);
        return vos;
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

    // ==================== 标签相关 ====================

    /** 单篇文章的标签引用（详情/保存回显用） */
    private List<TagRef> getTagRefs(Long articleId) {
        List<ArticleTag> links = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId));
        if (links.isEmpty()) {
            return List.of();
        }
        List<Long> tagIds = links.stream().map(ArticleTag::getTagId).distinct().collect(Collectors.toList());
        Map<Long, String> nameMap = tagMapper.selectByIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));
        // 按关联顺序输出
        return links.stream()
                .map(l -> {
                    String name = nameMap.get(l.getTagId());
                    return name == null ? null : new TagRef(l.getTagId(), name);
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 批量填充 VO 列表的标签：总共 2 条 SQL（关联 IN + 标签 IN），避免循环单查的 N+1 问题
     */
    private void fillTags(List<ArticleVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        List<Long> articleIds = vos.stream().map(ArticleVO::getId).collect(Collectors.toList());
        List<ArticleTag> links = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIds));
        if (links.isEmpty()) {
            vos.forEach(v -> v.setTags(List.of()));
            return;
        }
        List<Long> tagIds = links.stream().map(ArticleTag::getTagId).distinct().collect(Collectors.toList());
        Map<Long, String> nameMap = tagMapper.selectByIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));
        Map<Long, List<TagRef>> grouped = links.stream().collect(Collectors.groupingBy(
                ArticleTag::getArticleId,
                LinkedHashMap::new,
                Collectors.mapping(
                        l -> nameMap.get(l.getTagId()) == null ? null : new TagRef(l.getTagId(), nameMap.get(l.getTagId())),
                        Collectors.toList())));
        vos.forEach(v -> {
            List<TagRef> refs = grouped.get(v.getId());
            v.setTags(refs == null ? List.of()
                    : refs.stream().filter(java.util.Objects::nonNull).collect(Collectors.toList()));
        });
    }

    /**
     * 同步文章的标签关联（整表替换）：
     * 标签名规范化（去空/去重/限长/限量）→ 逐个复用或创建标签 → 删旧关联 → 插新关联
     */
    private void syncArticleTags(Long articleId, List<String> names) {
        List<String> normalized = normalizeTagNames(names);

        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId));
        for (String name : normalized) {
            ArticleTag link = new ArticleTag();
            link.setArticleId(articleId);
            link.setTagId(resolveOrCreateTag(name));
            try {
                articleTagMapper.insert(link);
            } catch (DuplicateKeyException e) {
                // uk_article_tag 兜底：并发下重复关联直接忽略
            }
        }
    }

    /** 标签名规范化：trim、去重、截断 32 字、最多 10 个，保留输入顺序 */
    private List<String> normalizeTagNames(List<String> names) {
        if (names == null) {
            return List.of();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String name : names) {
            if (name == null) {
                continue;
            }
            String t = name.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.length() > 32) {
                t = t.substring(0, 32);
            }
            set.add(t);
            if (set.size() >= 10) {
                break;
            }
        }
        return new ArrayList<>(set);
    }

    /**
     * 按名取标签，没有则创建。并发创建撞 uk_name 唯一索引时回查——
     * 和点赞表唯一索引兜底同一思路：不靠"先查后插"防并发，靠 DB 约束
     */
    private Long resolveOrCreateTag(String name) {
        Tag exist = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name));
        if (exist != null) {
            return exist.getId();
        }
        Tag tag = new Tag();
        tag.setName(name);
        try {
            tagMapper.insert(tag);
            return tag.getId();
        } catch (DuplicateKeyException e) {
            Tag again = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name));
            if (again != null) {
                return again.getId();
            }
            throw e;
        }
    }
}