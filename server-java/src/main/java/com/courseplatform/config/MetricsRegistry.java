package com.courseplatform.config;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 运行监控指标采集（教务端「运行监控」页数据源）。
 *
 * <p>对应原 Node 版 {@code server/db.js} 的内存 metrics：累计请求、异常请求、
 * 接口调用排行、SSE 在线连接数。教务端「重置演示数据」时一并清零。</p>
 */
@Component
public class MetricsRegistry {

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();
    private final AtomicLong sseClients = new AtomicLong();
    private final ConcurrentHashMap<String, AtomicLong> byPath = new ConcurrentHashMap<>();

    /** 记录一次接口调用（在鉴权拦截器入口调用，保证 401/403 也被计入）。 */
    public void recordRequest(String path) {
        totalRequests.incrementAndGet();
        byPath.computeIfAbsent(normalize(path), k -> new AtomicLong()).incrementAndGet();
    }

    /** 记录一次异常请求（由全局异常处理器调用）。 */
    public void recordError() {
        errors.incrementAndGet();
    }

    /** SSE 连接建立 / 断开。 */
    public void sseConnected() {
        sseClients.incrementAndGet();
    }

    public void sseDisconnected() {
        sseClients.updateAndGet(v -> v > 0 ? v - 1 : 0);
    }

    /** 重置演示数据时清零指标，与前端「重置演示数据」文案一致。 */
    public void reset() {
        totalRequests.set(0);
        errors.set(0);
        byPath.clear();
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getErrors() {
        return errors.get();
    }

    public long getSseClients() {
        return sseClients.get();
    }

    /** 接口调用排行（按调用次数降序，最多 limit 条）。 */
    public List<Map<String, Object>> ranking(int limit) {
        List<Map.Entry<String, AtomicLong>> entries = new ArrayList<>(byPath.entrySet());
        entries.sort(Comparator.comparingLong((Map.Entry<String, AtomicLong> e) -> e.getValue().get()).reversed());
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < entries.size() && i < limit; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("path", entries.get(i).getKey());
            m.put("count", entries.get(i).getValue().get());
            out.add(m);
        }
        return out;
    }

    /** 路径归一：把 /api/courses/12 收敛为 /api/courses/:id，避免排行被路径参数打散。 */
    private String normalize(String path) {
        if (path == null || path.isEmpty()) return "/";
        String[] seg = path.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < seg.length; i++) {
            if (seg[i].isEmpty()) continue;
            sb.append('/');
            if (i > 1 && seg[i].matches("\\d+")) sb.append(":id");
            else sb.append(seg[i]);
        }
        return sb.length() == 0 ? path : sb.toString();
    }
}
