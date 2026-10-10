package com.liuliu.example.myblogbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liuliu.example.myblogbackend.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
