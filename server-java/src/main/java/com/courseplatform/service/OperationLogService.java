package com.courseplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.courseplatform.entity.OperationLog;
import com.courseplatform.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 操作日志（审计）：统一写入与分页查询 */
@Service
public class OperationLogService {

    private final OperationLogMapper logMapper;

    public OperationLogService(OperationLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    /** 写入一条操作日志（ip 由调用方决定，默认空） */
    public void write(Long userId, String username, String action, String target, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setAction(action);
        log.setTarget(target);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        logMapper.insert(log);
    }

    /** 分页查询日志列表（page 从 1 开始，size 默认 20） */
    public Map<String, Object> list(Integer page, Integer size) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 20 : Math.min(size, 100);
        QueryWrapper<OperationLog> qw = new QueryWrapper<OperationLog>().orderByDesc("id");
        Long total = logMapper.selectCount(qw);
        List<OperationLog> rows = logMapper.selectList(qw.last("LIMIT " + (p - 1) * s + ", " + s));
        List<Map<String, Object>> items = new ArrayList<>();
        for (OperationLog log : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", log.getId());
            m.put("userId", log.getUserId());
            m.put("username", log.getUsername());
            m.put("action", log.getAction());
            m.put("target", log.getTarget());
            m.put("detail", log.getDetail());
            m.put("createdAt", log.getCreatedAt() == null ? "" : log.getCreatedAt().toString().replace("T", " "));
            items.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("total", total == null ? 0L : total);
        out.put("page", p);
        out.put("size", s);
        return out;
    }
}
