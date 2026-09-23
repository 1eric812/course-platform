package com.courseplatform.service;

import com.courseplatform.mapper.CoreMapper;
import com.courseplatform.support.Values;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 个人偏好服务（对应原 server/service.js 的 getPreferences / setPreferences）。
 * 主题 / 字体四档 / 信息密度 / 课表默认视图 + 四类消息订阅开关，落到 user_preference 表。
 */
@Service
public class PreferenceService {

    private final CoreMapper mapper;

    public PreferenceService(CoreMapper mapper) {
        this.mapper = mapper;
    }

    public Map<String, Object> get(long userId) {
        mapper.ensurePreference(userId);
        Map<String, Object> row = mapper.preference(userId);
        Map<String, Object> notify = new LinkedHashMap<>();
        notify.put("system", Values.asBool(Values.get(row, "notifySystem")));
        notify.put("result", Values.asBool(Values.get(row, "notifyResult")));
        notify.put("seat", Values.asBool(Values.get(row, "notifySeat")));
        notify.put("drop", Values.asBool(Values.get(row, "notifyDrop")));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("theme", Values.str(Values.get(row, "theme"), "light"));
        out.put("density", Values.str(Values.get(row, "density"), "standard"));
        out.put("fontScale", Values.str(Values.get(row, "fontScale"), "standard"));
        out.put("timetableView", Values.str(Values.get(row, "timetableView"), "week"));
        out.put("compactFilter", Values.asBool(Values.get(row, "compactFilter")));
        out.put("notify", notify);
        return out;
    }

    /** 局部更新：仅覆盖 patch 中出现的字段（等价原版浅合并语义）。 */
    public Map<String, Object> update(long userId, Map<String, Object> patch) {
        Map<String, Object> cur = get(userId);
        String theme = pickString(patch, "theme", Values.str(cur.get("theme"), "light"));
        String density = pickString(patch, "density", Values.str(cur.get("density"), "standard"));
        String fontScale = pickString(patch, "fontScale", Values.str(cur.get("fontScale"), "standard"));
        String timetableView = pickString(patch, "timetableView", Values.str(cur.get("timetableView"), "week"));
        boolean compactFilter = pickBool(patch, "compactFilter", Values.asBool(cur.get("compactFilter")));

        @SuppressWarnings("unchecked")
        Map<String, Object> curNotify = (Map<String, Object>) cur.get("notify");
        Object rawNotify = patch.get("notify");
        boolean nSystem = curNotify != null && Values.asBool(curNotify.get("system"));
        boolean nResult = curNotify != null && Values.asBool(curNotify.get("result"));
        boolean nSeat = curNotify != null && Values.asBool(curNotify.get("seat"));
        boolean nDrop = curNotify != null && Values.asBool(curNotify.get("drop"));
        if (rawNotify instanceof Map<?, ?> m) {
            if (m.containsKey("system")) {
                nSystem = Values.asBool(m.get("system"));
            }
            if (m.containsKey("result")) {
                nResult = Values.asBool(m.get("result"));
            }
            if (m.containsKey("seat")) {
                nSeat = Values.asBool(m.get("seat"));
            }
            if (m.containsKey("drop")) {
                nDrop = Values.asBool(m.get("drop"));
            }
            if (m.containsKey("remind")) {
                // 前端存在 remind 开关但库中无独立列，归并到系统提醒
                nSystem = Values.asBool(m.get("remind"));
            }
        }

        mapper.updatePreference(userId, theme, density, fontScale, timetableView,
                Values.asBoolInt(compactFilter), Values.asBoolInt(nResult), Values.asBoolInt(nSeat),
                Values.asBoolInt(nSystem), Values.asBoolInt(nDrop));
        return get(userId);
    }

    private static String pickString(Map<String, Object> patch, String key, String def) {
        Object v = patch == null ? null : patch.get(key);
        if (v == null) {
            return def;
        }
        String s = String.valueOf(v);
        return s.isBlank() ? def : s;
    }

    private static boolean pickBool(Map<String, Object> patch, String key, boolean def) {
        Object v = patch == null ? null : patch.get(key);
        return v == null ? def : Values.asBool(v);
    }
}
