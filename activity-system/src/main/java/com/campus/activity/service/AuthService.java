package com.campus.activity.service;

import com.campus.activity.dto.LoginDTO;
import com.campus.activity.dto.LoginVO;

/**
 * 鉴权 Service (登录)
 *
 * @author 勇闯天涯组
 */
public interface AuthService {

    /**
     * 账号密码登录
     *
     * @param dto 登录请求
     * @return token + 用户基本信息
     */
    LoginVO login(LoginDTO dto);
}
