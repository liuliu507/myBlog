package com.liuliu.example.myblogbackend.interceptor;

import tools.jackson.databind.ObjectMapper;
import com.liuliu.example.myblogbackend.common.Result;
import com.liuliu.example.myblogbackend.common.ErrorCode;
import com.liuliu.example.myblogbackend.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 明确公开的路径
        boolean isPublic = PUBLIC_PATHS.contains(uri) || isPublicGet(method, uri) || isPublicPost(method, uri);

        String auth = request.getHeader("Authorization");

        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Claims claims = jwtUtil.parseToken(token);
                request.setAttribute("userId", Long.valueOf(claims.getSubject()));
            } catch (Exception e) {
                writeError(response, ErrorCode.UNAUTHORIZED.getCode(), "登录已过期或无效");
                return false;
            }
        }

        // 公开路径放行；非公开路径必须有 userId
        if (isPublic) return true;
        if (request.getAttribute("userId") == null) {
            writeError(response, ErrorCode.UNAUTHORIZED.getCode(), "未登录");
            return false;
        }
        return true;
    }

    private void writeError(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, msg)));
    }
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/send-reset-code",
            "/api/auth/reset-password",
            "/api/user/test"
    );

    private boolean isPublicGet(String method, String uri) {
        if (!"GET".equalsIgnoreCase(method)) return false;
        // 公开浏览：文章列表/详情；个人专栏详情（含该专栏已发布文章）；文章评论树
        // 注意 GET /api/categories（我的分类列表）不在此列，仍需登录
        return uri.matches("^/api/articles(/\\d+)?$")
                || uri.matches("^/api/categories/\\d+$")
                || uri.matches("^/api/comments$");
    }

    private boolean isPublicPost(String method, String uri) {
        if (!"POST".equalsIgnoreCase(method)) return false;
        // 浏览计数对匿名用户开放（按 IP 防刷）
        return uri.matches("^/api/articles/\\d+/view$");
    }
}