package com.courseplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.courseplatform.entity.SelectionPeriod;
import org.apache.ibatis.annotations.Mapper;

/** 选课阶段表 Mapper（P-01-1 首页选课节点卡片数据来源） */
@Mapper
public interface SelectionPeriodMapper extends BaseMapper<SelectionPeriod> {
}
