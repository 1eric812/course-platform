package com.courseplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 业务参数（对应原 server/config.js）。
 * 演示参数与 Token 有效期来自 application.yml 的 course-platform.* 配置。
 */
@Component
@ConfigurationProperties(prefix = "course-platform")
public class AppProperties {

    private Demo demo = new Demo();
    private Auth auth = new Auth();
    private Assets assets = new Assets();

    public Demo getDemo() {
        return demo;
    }

    public void setDemo(Demo demo) {
        this.demo = demo;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Assets getAssets() {
        return assets;
    }

    public void setAssets(Assets assets) {
        this.assets = assets;
    }

    /** 演示行为参数：选课开放延迟 / SSE 推送周期 / 受理处理延迟。 */
    public static class Demo {
        private long openDelayMs = 90000L;
        private long seatTickMs = 4000L;
        private long processDelayMs = 900L;

        public long getOpenDelayMs() {
            return openDelayMs;
        }

        public void setOpenDelayMs(long openDelayMs) {
            this.openDelayMs = openDelayMs;
        }

        public long getSeatTickMs() {
            return seatTickMs;
        }

        public void setSeatTickMs(long seatTickMs) {
            this.seatTickMs = seatTickMs;
        }

        public long getProcessDelayMs() {
            return processDelayMs;
        }

        public void setProcessDelayMs(long processDelayMs) {
            this.processDelayMs = processDelayMs;
        }
    }

    /** 登录 Token 有效期（秒），默认 2 小时。 */
    public static class Auth {
        private long tokenTtlSeconds = 7200L;

        public long getTokenTtlSeconds() {
            return tokenTtlSeconds;
        }

        public void setTokenTtlSeconds(long tokenTtlSeconds) {
            this.tokenTtlSeconds = tokenTtlSeconds;
        }
    }

    /** 前端静态资源目录（可选）：配置后 /  与 /docs/** 由本服务托管。 */
    public static class Assets {
        private String clientDir = "";
        private String docsDir = "";

        public String getClientDir() {
            return clientDir;
        }

        public void setClientDir(String clientDir) {
            this.clientDir = clientDir;
        }

        public String getDocsDir() {
            return docsDir;
        }

        public void setDocsDir(String docsDir) {
            this.docsDir = docsDir;
        }
    }
}
