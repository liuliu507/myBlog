package com.liuliu.example.myblogbackend.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class RedisUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void set(String key, String value, long seconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(seconds));
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 按前缀批量删除。用 SCAN 渐进式扫描而不是 KEYS：
     * KEYS 一次遍历全部 key 会阻塞 Redis 主线程，key 多时造成线上卡顿；SCAN 分批游标遍历不阻塞
     */
    public void deleteByPrefix(String prefix) {
        ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(200).build();
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public boolean tryAcquire(String key, int maxCount, long windowSeconds) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }
        return count != null && count <= maxCount;
    }

    /** SET NX EX：key 不存在才写入并返回 true，已存在返回 false（原子操作，无并发竞态） */
    public boolean setIfAbsent(String key, String value, long seconds) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(seconds));
        return Boolean.TRUE.equals(ok);
    }
}
