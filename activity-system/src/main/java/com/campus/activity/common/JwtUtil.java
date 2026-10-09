package com.campus.activity.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 *
 * <p>对应需求文档 4.4 第 3 条: generate(account, role, name) / parse 带过期校验</p>
 * <p>使用 jjwt 0.12.x API</p>
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-hours:24}")
    private long expireHours;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // HS256 要求 key 至少 256 bit (32 字节)
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(bytes);
        log.info("JwtUtil initialized, expireHours={}", expireHours);
    }

    /**
     * 生成 token
     *
     * @param account 账号
     * @param role    STUDENT/TEACHER
     * @param name    姓名
     */
    public String generate(String account, String role, String name) {
        long now = System.currentTimeMillis();
        long exp = now + Duration.ofHours(expireHours).toMillis();

        Map<String, Object> claims = new HashMap<>();
        claims.put("account", account);
        claims.put("role", role);
        claims.put("name", name);

        return Jwts.builder()
                .claims(claims)
                .subject(account)
                .issuedAt(new Date(now))
                .expiration(new Date(exp))
                .signWith(key)
                .compact();
    }

    /**
     * 解析 token, 验证签名与过期时间
     *
     * @return Claims (含 account/role/name)
     * @throws ExpiredJwtException token 过期
     * @throws io.jsonwebtoken.JwtException 签名错误或格式非法
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验 token 是否合法且未过期
     */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT 已过期: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("JWT 校验失败: {}", e.getMessage());
            return false;
        }
    }
}
