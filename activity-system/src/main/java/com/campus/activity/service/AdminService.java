package com.campus.activity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.activity.dto.UserVO;

/**
 * 管理员 Service (V2.0 新增)
 *
 * <p>职责: 账号停用/启用、用户列表查询。
 * 管理员不介入报名业务。</p>
 *
 * @author 勇闯天涯组
 */
public interface AdminService {

    /**
     * 分页查询用户列表 (支持 role / status 筛选)
     *
     * @param role   角色过滤 (STUDENT/TEACHER/ADMIN), null 返回全部
     * @param status 状态过滤 (0/1), null 返回全部
     */
    IPage<UserVO> pageUsers(String role, Integer status, Long current, Long size);

    /**
     * 停用账号 (逻辑停用 status=0)
     * - 管理员账号不可停用
     * - 已停用抛 1013
     */
    void disable(String account);

    /**
     * 启用账号 (status=1)
     * - 已启用抛 1014
     */
    void enable(String account);
}
