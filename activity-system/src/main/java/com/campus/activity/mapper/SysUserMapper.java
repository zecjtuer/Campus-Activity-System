package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper: 单表 CRUD 由 MyBatis-Plus 提供
 *
 * @author 勇闯天涯组
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
