package com.campus.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.common.ResultCode;
import com.campus.activity.dto.UserVO;
import com.campus.activity.entity.SysUser;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.SysUserMapper;
import com.campus.activity.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 管理员 Service 实现 (V2.0 新增)
 *
 * @author 勇闯天涯组
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final SysUserMapper sysUserMapper;

    @Override
    public IPage<UserVO> pageUsers(String role, Integer status, Long current, Long size) {
        Page<SysUser> page = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .orderByAsc(SysUser::getRole)
                .orderByAsc(SysUser::getAccount);

        if (StringUtils.hasText(role)) {
            wrapper.eq(SysUser::getRole, role);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }

        IPage<SysUser> result = sysUserMapper.selectPage(page, wrapper);
        return result.convert(u -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(u, vo);
            return vo;
        });
    }

    @Override
    public void disable(String account) {
        SysUser user = mustLoadUser(account);

        // 管理员账号不可停用
        if (SysUser.ROLE_ADMIN.equals(user.getRole())) {
            throw new BusinessException(ResultCode.CANNOT_DISABLE_ADMIN);
        }
        if (user.getStatus() != null && user.getStatus() == SysUser.STATUS_DISABLED) {
            throw new BusinessException(ResultCode.ALREADY_DISABLED);
        }

        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getAccount, account)
                .set(SysUser::getStatus, SysUser.STATUS_DISABLED);
        sysUserMapper.update(null, wrapper);
        log.info("管理员停用账号: {}", account);
    }

    @Override
    public void enable(String account) {
        SysUser user = mustLoadUser(account);

        if (user.getStatus() != null && user.getStatus() == SysUser.STATUS_ENABLED) {
            throw new BusinessException(ResultCode.ALREADY_ENABLED);
        }

        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getAccount, account)
                .set(SysUser::getStatus, SysUser.STATUS_ENABLED);
        sysUserMapper.update(null, wrapper);
        log.info("管理员启用账号: {}", account);
    }

    private SysUser mustLoadUser(String account) {
        SysUser user = sysUserMapper.selectById(account);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }
}
