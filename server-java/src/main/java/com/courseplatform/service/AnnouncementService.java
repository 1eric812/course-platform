package com.courseplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.courseplatform.entity.Announcement;
import com.courseplatform.exception.ApiException;
import com.courseplatform.mapper.AnnouncementMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 公告：学生可见列表（置顶优先）、教务管理列表与 CRUD */
@Service
public class AnnouncementService {

    private final AnnouncementMapper announcementMapper;
    private final OperationLogService operationLogService;

    public AnnouncementService(AnnouncementMapper announcementMapper, OperationLogService operationLogService) {
        this.announcementMapper = announcementMapper;
        this.operationLogService = operationLogService;
    }

    private Map<String, Object> toMap(Announcement a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("title", a.getTitle());
        m.put("content", a.getContent());
        m.put("publisherId", a.getPublisherId());
        m.put("isPinned", a.getIsPinned() != null && a.getIsPinned() == 1);
        m.put("status", a.getStatus());
        m.put("publishedAt", a.getPublishedAt() == null ? "" : a.getPublishedAt().toString().replace("T", " "));
        m.put("createdAt", a.getCreatedAt() == null ? "" : a.getCreatedAt().toString().replace("T", " "));
        return m;
    }

    /** 学生可见：仅已发布，置顶优先，发布时间倒序 */
    public Map<String, Object> listPublished() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Announcement a : announcementMapper.selectList(new QueryWrapper<Announcement>()
                .eq("status", "PUBLISHED")
                .orderByDesc("is_pinned").orderByDesc("published_at"))) {
            items.add(toMap(a));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("total", items.size());
        return out;
    }

    /** 教务端：全部公告（含草稿/下线） */
    public Map<String, Object> adminList() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Announcement a : announcementMapper.selectList(new QueryWrapper<Announcement>().orderByDesc("id"))) {
            items.add(toMap(a));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("total", items.size());
        return out;
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        String title = str(body.get("title"));
        String content = str(body.get("content"));
        if (title.isEmpty()) throw new ApiException(400, "公告标题不能为空", null);
        if (content.isEmpty()) throw new ApiException(400, "公告内容不能为空", null);
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setContent(content);
        a.setPublisherId(body.get("publisherId") == null ? null : Long.valueOf(String.valueOf(body.get("publisherId"))));
        a.setIsPinned(Boolean.TRUE.equals(body.get("isPinned")) ? 1 : 0);
        a.setStatus(str(body.get("status")).isEmpty() ? "PUBLISHED" : str(body.get("status")));
        a.setPublishedAt(LocalDateTime.now());
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        announcementMapper.insert(a);
        operationLogService.write(42L, "admin", "公告发布", title, "发布公告（id=" + a.getId() + "）");
        return toMap(a);
    }

    /** 更新公告（含下线：status=OFFLINE） */
    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null) throw new ApiException(404, "公告不存在", null);
        if (body.get("title") != null) a.setTitle(str(body.get("title")));
        if (body.get("content") != null) a.setContent(str(body.get("content")));
        if (body.get("isPinned") != null) a.setIsPinned(Boolean.TRUE.equals(body.get("isPinned")) ? 1 : 0);
        if (body.get("status") != null) a.setStatus(str(body.get("status")));
        if (body.get("publishedAt") != null) {
            try { a.setPublishedAt(LocalDateTime.parse(String.valueOf(body.get("publishedAt")).replace(' ', 'T'))); } catch (Exception ignore) { }
        }
        a.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(a);
        operationLogService.write(42L, "admin", "公告更新", a.getTitle(), "更新公告（id=" + id + "）");
        return toMap(a);
    }

    @Transactional
    public Map<String, Object> delete(Long id) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null) throw new ApiException(404, "公告不存在", null);
        announcementMapper.deleteById(id);
        operationLogService.write(42L, "admin", "公告删除", a.getTitle(), "删除公告（id=" + id + "）");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        return out;
    }

    private String str(Object v) { return v == null ? "" : String.valueOf(v).trim(); }
}
