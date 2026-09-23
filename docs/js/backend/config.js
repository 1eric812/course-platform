/* 静态版配置（浏览器环境，去掉了 process.env） */

export const config = {
  // 演示用：页面加载后 N 毫秒"开放选课"，便于观察倒计时
  openDelayMs: 90_000,
  // 名额模拟变动周期
  seatTickMs: 4_000,
  // 选课受理的模拟异步延迟（体现「受理回执 + 结果推送」）
  processDelayMs: 900,
};
