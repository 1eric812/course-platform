package com.courseplatform.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 退出登录 Token 黑名单（内存版）。
 *
 * <p>JWT 本身无状态，签发后无法作废；为满足「退出后 token 失效」需求
 * （与原 Node 版行为一致），登出时把 token 存入本黑名单，
 * 鉴权拦截器命中即返回 401。条目随其自然过期时间惰性清理，无需后台任务。</p>
 */
@Component
public class TokenBlacklist {

    private final Map<String, Long> revokedUntil = new ConcurrentHashMap<>();

    /** 作废 token，untilMs 为该 token 的自然过期时间（毫秒） */
    public void revoke(String token, long untilMs) {
        if (token != null && !token.isBlank()) revokedUntil.put(token, untilMs);
    }

    /** token 是否已被作废（过期条目顺带清理） */
    public boolean isRevoked(String token) {
        Long until = revokedUntil.get(token);
        if (until == null) return false;
        if (until < System.currentTimeMillis()) {
            revokedUntil.remove(token);
            return false;
        }
        return true;
    }
}
