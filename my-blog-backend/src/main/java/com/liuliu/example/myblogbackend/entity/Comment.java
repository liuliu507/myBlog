package com.liuliu.example.myblogbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long userId;
    /** 父评论 ID（null = 直接评论文章，非 null = 回复某条评论） */
    private Long parentId;
    private String content;
    private LocalDateTime createdAt;
}
