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
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status_created` (`status`, `created_at`)
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

