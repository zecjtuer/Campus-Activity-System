package com.campus.activity.interceptor;

import com.campus.activity.common.JwtUtil;
import com.campus.activity.common.LoginUser;
import com.campus.activity.common.Result;
import com.campus.activity.common.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器
 *
 * <p>对应需求文档 4.4 第 4 条:
 * <ol>
 *   <li>从 Authorization: Bearer 头取 token</li>
 *   <li>解析后把 account/role 放入 ThreadLocal&lt;LoginUser&gt;</li>
 *   <li>无 token / 过期 / 非法 → 401</li>
 *   <li>afterCompletion 中清理 ThreadLocal 防止内存泄漏</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return writeUnauthorized(response, "缺少 Authorization 头");
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            return writeUnauthorized(response, "Token 为空");
        }

        try {
            Claims claims = jwtUtil.parse(token);
            LoginUser user = new LoginUser(
                    claims.get("account", String.class),
                    claims.get("role", String.class),
                    claims.get("name", String.class)
            );
            LoginUser.set(user);
            return true;
        } catch (Exception e) {
            log.warn("Token 校验失败: {}", e.getMessage());
            return writeUnauthorized(response, "Token 无效或已过期");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 清理 ThreadLocal 防止内存泄漏
        LoginUser.clear();
    }

    private boolean writeUnauthorized(HttpServletResponse response, String detail) throws Exception {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Result<Void> body = Result.fail(ResultCode.UNAUTHORIZED.getCode(),
                ResultCode.UNAUTHORIZED.getMessage() + ": " + detail);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }
}
