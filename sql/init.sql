-- 强制本次导入会话使用 utf8mb4（规避 MySQL 8.4 客户端 auto 字符集
-- 在容器 POSIX locale 下回退 latin1 导致的双重编码问题）
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS my_blog
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE my_blog;

CREATE TABLE `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `email`      VARCHAR(100) NOT NULL COMMENT '登录邮箱',
  `password`   VARCHAR(100) NOT NULL COMMENT '密码',
  `nickname`   VARCHAR(50)  NOT NULL COMMENT '昵称',
  `avatar`     VARCHAR(255) DEFAULT NULL COMMENT '头像',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `category` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT      NOT NULL COMMENT '所属用户',
  `name`       VARCHAR(50) NOT NULL COMMENT '分类名',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

CREATE TABLE `article` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL COMMENT '作者',
  `category_id` BIGINT       DEFAULT NULL COMMENT '分类',
  `title`       VARCHAR(200) NOT NULL COMMENT '标题',
  `summary`     VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `content`     TEXT         NOT NULL COMMENT '正文',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '0草稿 1已发布',
  `view_count`  INT          NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status_created` (`status`, `created_at`),
  -- 全文索引（ngram 中文分词，默认二元切词），供 MATCH...AGAINST 搜索标题/摘要/正文
  FULLTEXT KEY `ft_article_search` (`title`, `summary`, `content`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

CREATE TABLE `email_code` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `email`      VARCHAR(100) NOT NULL COMMENT '邮箱',
  `code`       VARCHAR(10) NOT NULL COMMENT '验证码',
  `type`       VARCHAR(20) NOT NULL DEFAULT 'reset_password' COMMENT '用途',
  `expire_at`  DATETIME    NOT NULL COMMENT '过期时间',
  `used`       TINYINT     NOT NULL DEFAULT 0 COMMENT '0未用 1已用',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_email_type` (`email`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮箱验证码表';

CREATE TABLE `comment` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `article_id` BIGINT      NOT NULL COMMENT '所属文章',
  `user_id`    BIGINT      NOT NULL COMMENT '评论人',
  `parent_id`  BIGINT      DEFAULT NULL COMMENT '父评论ID（null=直接评论文章，否则为回复）',
  `content`    TEXT        NOT NULL COMMENT '评论内容',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_article_created` (`article_id`, `created_at`) COMMENT '按文章+时间查评论'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表（二级）';

CREATE TABLE `article_like` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `article_id` BIGINT   NOT NULL COMMENT '文章',
  `user_id`    BIGINT   NOT NULL COMMENT '点赞人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_user` (`article_id`, `user_id`) COMMENT '防重复点赞：DB 唯一约束兜底',
  KEY `idx_article` (`article_id`) COMMENT '统计文章点赞数'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章点赞表';

CREATE TABLE `chat_message` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT      NOT NULL COMMENT '所属用户',
  `role`       VARCHAR(16) NOT NULL COMMENT '消息角色 user/assistant',
  `content`    TEXT        NOT NULL COMMENT '消息内容',
  `article_id` BIGINT      DEFAULT NULL COMMENT '关联文章（文章页对话时）',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`, `created_at`) COMMENT '按用户+时间查历史'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话记录';

CREATE TABLE `tag` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`       VARCHAR(32) NOT NULL COMMENT '标签名（全局共享，全站唯一）',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表（全局）';

CREATE TABLE `article_tag` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `article_id` BIGINT   NOT NULL COMMENT '文章',
  `tag_id`     BIGINT   NOT NULL COMMENT '标签',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_tag` (`article_id`, `tag_id`) COMMENT '防重复关联',
  KEY `idx_tag` (`tag_id`) COMMENT '按标签反查文章'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章-标签关联表';

CREATE TABLE `notification` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`       BIGINT       NOT NULL COMMENT '接收者（文章作者/被回复人）',
  `sender_id`     BIGINT       NOT NULL COMMENT '触发者',
  `type`          VARCHAR(16)  NOT NULL COMMENT '通知类型 comment/reply/like',
  `article_id`    BIGINT       NOT NULL COMMENT '关联文章',
  `article_title` VARCHAR(200) DEFAULT NULL COMMENT '文章标题快照（文章删除后仍可展示）',
  `comment_text`  VARCHAR(200) DEFAULT NULL COMMENT '评论内容快照（点赞通知为空）',
  `is_read`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读 0未读 1已读',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `is_read`, `id`) COMMENT '覆盖未读数统计与通知列表分页'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内消息通知表';

INSERT INTO `user` (`email`, `password`, `nickname`)
VALUES ('test@example.com', 'placeholder_will_be_replaced', '测试用户');

INSERT INTO `category` (`user_id`, `name`)
VALUES (1, '学习笔记');

INSERT INTO `article` (`user_id`, `category_id`, `title`, `summary`, `content`, `status`)
VALUES (
  1,
  1,
  '我的第一篇文章',
  '这是摘要',
  '# 你好\n这是我用 Vue3 + Spring Boot 写的第一篇博客。',
  1
);

