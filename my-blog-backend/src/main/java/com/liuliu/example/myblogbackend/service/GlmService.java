package com.liuliu.example.myblogbackend.service;

import com.liuliu.example.myblogbackend.common.ErrorCode;
import com.liuliu.example.myblogbackend.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GlmService {

    private final RestClient restClient;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${glm.api-key}")
    private String apiKey;

    @Value("${glm.model:glm-4-flash}")
    private String model;

    public GlmService(@Value("${glm.base-url}") String baseUrl, ObjectMapper objectMapper) {
        // 用静态 builder 自己构造，不依赖自动装配的 RestClient.Builder bean（webmvc starter 未装配）
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    /** 组装请求体（system + user），非流式/流式共用 */
    private Map<String, Object> buildBody(String userMessage, String articleTitle, String articleContent) {
        List<Map<String, String>> messages = new ArrayList<>();

        // 注入文章阅读上下文，让 AI 能“读懂”用户正在看的文章
        if (articleContent != null && !articleContent.isBlank()) {
            String system = "你是文章阅读助手。用户正在阅读以下文章，请优先基于文章内容回答问题：\n"
                    + "标题：" + (articleTitle == null ? "" : articleTitle) + "\n"
                    + "正文：" + articleContent;
            Map<String, String> sys = new LinkedHashMap<>();
            sys.put("role", "system");
            sys.put("content", system);
            messages.add(sys);
        }

        Map<String, String> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", userMessage);
        messages.add(user);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        return body;
    }

    /**
     * 非流式对话：调用智谱 GLM-4-Flash，返回完整回复。
     */
    public String chat(String userMessage, String articleTitle, String articleContent) {
        Map<String, Object> body = buildBody(userMessage, articleTitle, articleContent);
        body.put("stream", false);

        try {
            JsonNode resp = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (resp == null) {
                throw new BusinessException(ErrorCode.ERROR, "AI 返回内容为空");
            }
            JsonNode content = resp.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new BusinessException(ErrorCode.ERROR, "AI 返回内容为空");
            }
            return content.asText();
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("调用智谱 GLM 失败", e);
            throw new BusinessException(ErrorCode.ERROR, "AI 服务暂时不可用，请稍后再试");
        }
    }

    /**
     * 流式对话：返回每个 token 分片，供前端逐字显示（打字机效果）。
     * 智谱 SSE 每个事件 data 是 JSON：{"choices":[{"delta":{"content":"你"}}]}
     * 结束标记为 data: [DONE]
     */
    public Flux<ChatDelta> streamChat(String userMessage, String articleTitle, String articleContent) {
        Map<String, Object> body = buildBody(userMessage, articleTitle, articleContent);
        body.put("stream", true);

        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        resp -> Mono.error(new BusinessException(ErrorCode.ERROR, "AI 服务暂时不可用，请稍后再试")))
                .bodyToFlux(String.class)
                .handle((raw, sink) -> {
                    if (raw == null || raw.isBlank() || "[DONE]".equals(raw)) {
                        return; // 跳过结束标记
                    }
                    try {
                        JsonNode node = objectMapper.readValue(raw, JsonNode.class);
                        JsonNode content = node.path("choices").path(0).path("delta").path("content");
                        if (!content.isMissingNode() && !content.asText().isEmpty()) {
                            sink.next(new ChatDelta(content.asText()));
                        }
                    } catch (Exception e) {
                        log.warn("解析 GLM SSE 分片失败: {}", raw);
                    }
                });
    }

    /** 流式回复的单个分片 */
    public record ChatDelta(String text) {}
}
