package com.courseplatform.config;

import org.springframework.stereotype.Component;

/**
 * 服务启动时间与「选课开放时刻」统一口径。
 *
 * <p>原实现中 /api/status、SSE 推送、健康检查各自持有独立的启动时间戳，存在毫秒级漂移；
 * 需求强调「结果与名额的确定性反馈」（I-05 / I-06），故统一由本组件提供唯一时间基准，
 * 开放延迟取自 application.yml 的 course-platform.demo.open-delay-ms。</p>
 */
@Component
public class ServerClock {

    private final long startedAt = System.currentTimeMillis();
    private final AppProperties props;

    public ServerClock(AppProperties props) {
        this.props = props;
    }

    /** 服务启动时刻（毫秒）。 */
    public long startedAt() {
        return startedAt;
    }

    /** 已运行秒数。 */
    public long uptimeSeconds() {
        return (System.currentTimeMillis() - startedAt) / 1000;
    }

    /** 选课开放时刻（启动时间 + 演示延迟）。 */
    public long openAt() {
        return startedAt + props.getDemo().getOpenDelayMs();
    }
}
