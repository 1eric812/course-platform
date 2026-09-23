/* 静态版配置（浏览器环境，去掉了 process.env） */

export const config = {
  /**
   * 演示用：首次进入时把「正选」阶段安排在 N 毫秒后开始，便于观察倒计时。
   *
   * 注意这不是倒计时的直接锚点——真正的开放时刻由 selection_period（见 data.js 的
   * periodState）推导，本值只用于播种演示阶段数据。教务端调整阶段时间后，
   * 倒计时会跟着数据库配置走，而不是跟着这个常量走。
   */
  openDelayMs: 90_000,
  // 名额模拟变动周期
  seatTickMs: 4_000,
  // 选课受理的模拟异步延迟（体现「受理回执 + 结果推送」）
  processDelayMs: 900,
};
