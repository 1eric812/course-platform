package com.courseplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.courseplatform.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
