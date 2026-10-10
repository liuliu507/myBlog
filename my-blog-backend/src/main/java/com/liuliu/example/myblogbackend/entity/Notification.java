package com.liuliu.example.myblogbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 接收者（文章作者/被回复人） */
    private Long userId;
    /** 触发者 */
    private Long senderId;
    /** 通知类型：comment / like */
    private String type;
    private Long articleId;
    /** 文章标题快照（文章删除后仍可展示） */
    private String articleTitle;
    /** 评论内容快照（点赞通知为空） */
    private String commentText;
    /** 0 未读 / 1 已读 */
    private Integer isRead;
    private LocalDateTime createdAt;
}
