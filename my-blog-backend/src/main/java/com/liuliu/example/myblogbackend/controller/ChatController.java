package com.liuliu.example.myblogbackend.controller;

import com.liuliu.example.myblogbackend.common.ErrorCode;
import com.liuliu.example.myblogbackend.common.Result;
import com.liuliu.example.myblogbackend.dto.ChatMessageVO;
import com.liuliu.example.myblogbackend.dto.ChatRequest;
import com.liuliu.example.myblogbackend.dto.ChatVO;
import com.liuliu.example.myblogbackend.exception.BusinessException;
import com.liuliu.example.myblogbackend.service.ChatMessageService;
import com.liuliu.example.myblogbackend.service.GlmService;
import com.liuliu.example.myblogbackend.util.RedisUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    // 每用户每分钟最多提问条数
    private static final int RATE_LIMIT = 10;
    private static final int RATE_WINDOW_SECONDS = 60;
    // 历史回显条数
    private static final int HISTORY_LIMIT = 50;

    @Autowired
    private GlmService glmService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ChatMessageService chatMessageService;

    @PostMapping
    public Result<ChatVO> chat(@RequestAttribute("userId") Long userId,
                               @Valid @RequestBody ChatRequest req) {
        // 限流：复用 RedisUtil 计数器（increment + expire，原子计数）
        String limitKey = "chat:limit:" + userId;
        if (!redisUtil.tryAcquire(limitKey, RATE_LIMIT, RATE_WINDOW_SECONDS)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS,
                    "提问太频繁了，每分钟最多 " + RATE_LIMIT + " 条");
        }

        // 落库：用户提问
        chatMessageService.save(userId, "user", req.getMessage(), req.getArticleId());

        String reply = glmService.chat(req.getMessage(),
                req.getArticleTitle(), req.getArticleContent());

        // 落库：AI 回复
        chatMessageService.save(userId, "assistant", reply, req.getArticleId());

        return Result.success(new ChatVO(reply));
    }

    /**
     * 流式对话：返回 SSE，每个事件 data 为 {"text":"..."}，前端逐字拼接。
     * 落库策略：用户提问先存（同步），AI 回复在流结束后累加存库（异步切线程池，避免阻塞 Netty event loop）。
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<GlmService.ChatDelta> chatStream(@RequestAttribute("userId") Long userId,
                                                 @Valid @RequestBody ChatRequest req) {
        String limitKey = "chat:limit:" + userId;
        if (!redisUtil.tryAcquire(limitKey, RATE_LIMIT, RATE_WINDOW_SECONDS)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS,
                    "提问太频繁了，每分钟最多 " + RATE_LIMIT + " 条");
        }

        // 落库：用户提问（servlet 线程同步落库没问题）
        chatMessageService.save(userId, "user", req.getMessage(), req.getArticleId());

        StringBuilder acc = new StringBuilder();
        return glmService.streamChat(req.getMessage(), req.getArticleTitle(), req.getArticleContent())
                .doOnNext(delta -> acc.append(delta.text()))
                .doOnComplete(() -> {
                    String reply = acc.toString();
                    if (!reply.isBlank()) {
                        // 切到适合阻塞 IO 的线程池落库，避免阻塞 WebClient 的 Netty event loop
                        Schedulers.boundedElastic().schedule(() ->
                                chatMessageService.save(userId, "assistant", reply, req.getArticleId()));
                    }
                });
    }

    /**
     * 拉取当前用户最近的对话记录（打开抽屉时回显历史）。
     */
    @GetMapping("/history")
    public Result<List<ChatMessageVO>> history(@RequestAttribute("userId") Long userId) {
        return Result.success(chatMessageService.listRecent(userId, HISTORY_LIMIT));
    }
}
