package com.courseplatform.service;

import com.courseplatform.mapper.CoreMapper;
import com.courseplatform.support.ApiException;
import com.courseplatform.support.Values;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.courseplatform.support.Values.asDouble;
import static com.courseplatform.support.Values.asInt;
import static com.courseplatform.support.Values.asLong;
import static com.courseplatform.support.Values.str;

/**
 * 心愿单服务（对应原 server/service.js 的 wishlist* 系列）。
 * 支持志愿序（priority）排序、上移/下移（reorder）、冲突预标记（blocked）。
 */
@Service
public class WishlistService {

    private final CoreMapper mapper;
    private final CourseService courseService;

    public WishlistService(CoreMapper mapper, CourseService courseService) {
        this.mapper = mapper;
        this.courseService = courseService;
    }

    /** 心愿单明细：按志愿序返回课程完整信息 + 冲突明细。 */
    public Map<String, Object> detail(long studentId) {
        List<Map<String, Object>> rows = mapper.wishlist(studentId);
        Map<Long, Map<String, Object>> courseMap = new java.util.LinkedHashMap<>();
        for (Map<String, Object> c : courseService.catalog()) {
            courseMap.put(asLong(c.get("id")), c);
        }
        List<Map<String, Object>> enrolled = courseService.enrolledCourses(studentId);
        Map<String, Object> rules = mapper.rules();
        double used = courseService.sumCredits(enrolled);
        double limit = rules == null ? 30d : asDouble(rules.get("creditLimit"), 30d);

        List<Map<String, Object>> items = new ArrayList<>();
        double planned = used;
        for (Map<String, Object> row : rows) {
            long cid = asLong(row.get("courseId"));
            Map<String, Object> course = courseMap.get(cid);
            if (course == null) {
                continue;
            }
            List<Map<String, Object>> conflicts = courseService.detectConflicts(course, enrolled, rules, planned);
            planned = Values.round1(planned + asDouble(course.get("credits")));
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("courseId", cid);
            item.put("priority", asInt(row.get("priority")));
            item.put("course", course);
            item.put("conflict", conflicts);
            item.put("blocked", !courseService.hardOnly(conflicts).isEmpty());
            items.add(item);
        }
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("items", items);
        out.put("maxWishlist", rules == null ? 8 : asInt(rules.get("maxWishlist")));
        out.put("creditLimit", limit);
        return out;
    }

    /** 加入心愿单（末位志愿）。 */
    @Transactional
    public Map<String, Object> add(long studentId, long courseId) {
        Map<String, Object> course = mapper.courseById(courseId);
        if (course == null) {
            throw ApiException.notFound("课程不存在");
        }
        Map<String, Object> rules = mapper.rules();
        int max = rules == null ? 8 : asInt(rules.get("maxWishlist"));
        if (mapper.isEnrolled(studentId, courseId) > 0) {
            throw ApiException.badRequest("该课程已在你的课表中，无需重复加入心愿单");
        }
        if (mapper.inWishlist(studentId, courseId) > 0) {
            throw ApiException.badRequest("该课程已在心愿单中");
        }
        if (mapper.wishlistSize(studentId) >= max) {
            throw ApiException.badRequest("心愿单最多 " + max + " 门，请先移除部分课程");
        }
        mapper.wishlistAdd(studentId, courseId, mapper.maxPriority(studentId) + 1);
        return detail(studentId);
    }

    /** 移出心愿单，并重排剩余志愿序。 */
    @Transactional
    public Map<String, Object> remove(long studentId, long courseId) {
        mapper.wishlistRemove(studentId, courseId);
        resequence(studentId);
        return detail(studentId);
    }

    /** 按前端给定的顺序重排志愿序（orderedIds 为课程 id 顺序）。 */
    @Transactional
    public Map<String, Object> reorder(long studentId, List<Object> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw ApiException.badRequest("排序结果不能为空");
        }
        Set<Long> mine = new LinkedHashSet<>();
        for (Map<String, Object> r : mapper.wishlist(studentId)) {
            mine.add(asLong(r.get("courseId")));
        }
        int idx = 1;
        for (Object o : orderedIds) {
            long cid = asLong(o);
            if (mine.contains(cid)) {
                mapper.wishlistSetPriority(studentId, cid, idx++);
            }
        }
        resequence(studentId);
        return detail(studentId);
    }

    /** 清空心愿单（提交成功后由选课流程调用）。 */
    @Transactional
    public void clear(long studentId) {
        mapper.wishlistClear(studentId);
    }

    private void resequence(long studentId) {
        List<Map<String, Object>> rows = mapper.wishlist(studentId);
        int idx = 1;
        for (Map<String, Object> r : rows) {
            long cid = asLong(r.get("courseId"));
            if (asInt(r.get("priority")) != idx) {
                mapper.wishlistSetPriority(studentId, cid, idx);
            }
            idx++;
        }
    }

    /** 心愿单中的课程 id（按志愿序）。 */
    public List<Long> courseIds(long studentId) {
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> r : mapper.wishlist(studentId)) {
            ids.add(asLong(r.get("courseId")));
        }
        return ids;
    }

    public String nameOf(long courseId) {
        Map<String, Object> c = mapper.courseById(courseId);
        return c == null ? ("课程#" + courseId) : str(c.get("name"), "课程#" + courseId);
    }
}
