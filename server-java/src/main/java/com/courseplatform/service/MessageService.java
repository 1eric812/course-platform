package com.courseplatform.service;

import com.courseplatform.mapper.CoreMapper;
import com.courseplatform.support.Values;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息中心服务（对应原 server/service.js 的 messages / markMessagesRead / pushMessage）。
 * 库里 type 存大写枚举，输出时统一转小写，与前端 typeName 映射（system/result/seat/remind/drop）一致。
 */
@Service
public class MessageService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final CoreMapper mapper;

    public MessageService(CoreMapper mapper) {
        this.mapper = mapper;
    }

    /** 消息列表（新→旧）。 */
    public List<Map<String, Object>> items(long userId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : mapper.messages(userId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", Values.asLong(r.get("id")));
            m.put("type", Values.str(r.get("type"), "system").toLowerCase());
            m.put("title", Values.str(r.get("title"), ""));
            m.put("body", Values.str(r.get("body"), ""));
            m.put("read", Values.asBool(r.get("isRead")));
            m.put("time", formatTime(r.get("createdAt")));
            out.add(m);
        }
        return out;
    }

    /** 未读数（前端顶栏角标）。 */
    public int unread(long userId) {
        int n = 0;
        for (Map<String, Object> m : items(userId)) {
            if (!Boolean.TRUE.equals(m.get("read"))) {
                n++;
            }
        }
        return n;
    }

    public int markRead(long userId) {
        return mapper.markMessagesRead(userId);
    }

    /** 推送一条站内消息（type 传小写，落库存大写）。 */
    public void push(long userId, String type, String title, String body) {
        if (userId <= 0) {
            return;
        }
        mapper.addMessage(userId, Values.str(type, "SYSTEM").toUpperCase(), title, body);
    }

    private static String formatTime(Object o) {
        if (o == null) {
            return "";
        }
        if (o instanceof LocalDateTime dt) {
            return dt.format(TIME_FMT);
        }
        if (o instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().format(TIME_FMT);
        }
        String s = String.valueOf(o);
        return s.length() >= 16 ? s.substring(0, 16) : s;
    }
}
