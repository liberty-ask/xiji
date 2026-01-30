package com.xiji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiji.entity.domain.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收支类别Mapper接口
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}

