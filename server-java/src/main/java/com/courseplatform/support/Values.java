package com.courseplatform.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 类型转换工具：MyBatis 注解查询返回 {@code Map<String,Object>}，
 * 各驱动对 DECIMAL / TINYINT 的映射类型不一致（BigDecimal / Integer / Long / Boolean），
 * 统一在此收敛，保证响应 JSON 与原 Node 版接口一致。
 */
public final class Values {

    private Values() {
    }

    public static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    public static String str(Object o, String def) {
        String s = str(o);
        return s == null || s.isBlank() ? def : s;
    }

    public static long asLong(Object o) {
        if (o instanceof Number n) {
            return n.longValue();
        }
        if (o == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public static int asInt(Object o) {
        return (int) asLong(o);
    }

    public static double asDouble(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        if (o == null) {
            return 0d;
        }
        try {
            return Double.parseDouble(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    public static double asDouble(Object o, double def) {
        if (o == null) {
            return def;
        }
        return asDouble(o);
    }

    public static boolean asBool(Object o) {
        if (o instanceof Boolean b) {
            return b;
        }
        if (o instanceof Number n) {
            return n.intValue() != 0;
        }
        String s = str(o);
        return s != null && ("1".equals(s) || "true".equalsIgnoreCase(s));
    }

    public static int asBoolInt(Object o) {
        return asBool(o) ? 1 : 0;
    }

    /** 逗号分隔字符串 → 去空列表。 */
    public static List<String> splitCsv(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split(",")) {
            String v = part.trim();
            if (!v.isEmpty()) {
                out.add(v);
            }
        }
        return out;
    }

    /** 请求参数按逗号拆分为字符串集合（为空的参数返回空集合）。 */
    public static List<String> csvParam(String raw) {
        return splitCsv(raw);
    }

    public static List<Integer> intCsvParam(String raw) {
        List<Integer> out = new ArrayList<>();
        for (String s : splitCsv(raw)) {
            try {
                out.add((int) Double.parseDouble(s));
            } catch (NumberFormatException ignore) {
                // 忽略非法值
            }
        }
        return out;
    }

    /** 空安全取值。 */
    public static Object get(Map<String, Object> row, String key) {
        return row == null ? null : row.get(key);
    }

    public static double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    /** 用于日志/调试的数组拼接。 */
    public static String join(Object... parts) {
        return Arrays.toString(parts);
    }
}
