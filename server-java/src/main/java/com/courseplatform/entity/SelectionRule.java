package com.courseplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 教务选课规则（单行配置） */
@TableName("selection_rule")
public class SelectionRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer selectionOpen;
    private BigDecimal creditLimit;
    private Integer maxWishlist;
    private Integer allowCrossCampus;
    private Integer blockOnConflict;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getSelectionOpen() { return selectionOpen; }
    public void setSelectionOpen(Integer selectionOpen) { this.selectionOpen = selectionOpen; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    public Integer getMaxWishlist() { return maxWishlist; }
    public void setMaxWishlist(Integer maxWishlist) { this.maxWishlist = maxWishlist; }
    public Integer getAllowCrossCampus() { return allowCrossCampus; }
    public void setAllowCrossCampus(Integer allowCrossCampus) { this.allowCrossCampus = allowCrossCampus; }
    public Integer getBlockOnConflict() { return blockOnConflict; }
    public void setBlockOnConflict(Integer blockOnConflict) { this.blockOnConflict = blockOnConflict; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
