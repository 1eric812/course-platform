package com.courseplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.courseplatform.config.MetricsRegistry;
import com.courseplatform.config.ServerClock;
import com.courseplatform.entity.Course;
import com.courseplatform.mapper.CourseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 名额实时推送（SSE） */
@RestController
@Component
public class StreamController {

    private static final Logger log = LoggerFactory.getLogger(StreamController.class);
    private final CourseMapper courseMapper;
    private final long startedAt = System.currentTimeMillis();
    private final Set<SseEmitter> clients = ConcurrentHashMap.newKeySet();

    public StreamController(CourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    @GetMapping("/api/stream/seats")
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(0L); // 不超时
        clients.add(emitter);
        emitter.onCompletion(() -> clients.remove(emitter));
        emitter.onTimeout(() -> clients.remove(emitter));
        emitter.onError(t -> clients.remove(emitter));
        try {
            Map<String, Object> hello = new LinkedHashMap<>();
            hello.put("type", "hello");
            hello.put("serverTime", System.currentTimeMillis());
            hello.put("openAt", startedAt + 90000);
            emitter.send(SseEmitter.event().data(hello));
        } catch (Exception e) {
            clients.remove(emitter);
        }
        return emitter;
    }

    @Scheduled(fixedDelay = 4000)
    public void tick() {
        if (clients.isEmpty()) return;
        List<Course> cs = courseMapper.selectList(null);
        if (!cs.isEmpty()) {
            // 随机消耗若干课程名额
            int n = 1 + (int) (Math.random() * 3);
            for (int i = 0; i < n && !cs.isEmpty(); i++) {
                Course c = cs.get((int) (Math.random() * cs.size()));
                if (c.getRemaining() > 0 && Math.random() < 0.55) {
                    c.setEnrolled(c.getEnrolled() + 1);
                    courseMapper.updateById(c);
                }
            }
        }
        List<Map<String, Object>> seats = new ArrayList<>();
        for (Course c : courseMapper.selectList(null)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("remaining", c.getRemaining());
            m.put("capacity", c.getCapacity());
            seats.add(m);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "seats");
        payload.put("serverTime", System.currentTimeMillis());
        payload.put("openAt", startedAt + 90000);
        payload.put("seats", seats);
        for (SseEmitter e : clients) {
            try { e.send(SseEmitter.event().data(payload)); }
            catch (Exception ex) { clients.remove(e); }
        }
    }
}
