package com.courseplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 选课阶段（预选 / 正选 / 补退选）。
 *
 * <p>对应需求 P-01-1「选课节点卡片」：首页展示阶段名称、开放起止时间与倒计时；
 * 数据来自 schema.sql 第 15 张表 selection_period，演示数据见 data.sql。</p>
 */
@TableName("selection_period")
public class SelectionPeriod {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 阶段编码：PRE / MAIN / ADJUST */
    private String phaseCode;
    /** 阶段名称：预选 / 正选 / 补退选 */
    private String phaseName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 状态：NOT_STARTED / ONGOING / FINISHED */
    private String status;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPhaseCode() { return phaseCode; }
    public void setPhaseCode(String phaseCode) { this.phaseCode = phaseCode; }
    public String getPhaseName() { return phaseName; }
    public void setPhaseName(String phaseName) { this.phaseName = phaseName; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
