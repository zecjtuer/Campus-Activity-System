package com.campus.activity.service.impl;

import com.campus.activity.common.JwtUtil;
import com.campus.activity.common.ResultCode;
import com.campus.activity.dto.LoginDTO;
import com.campus.activity.dto.LoginVO;
import com.campus.activity.entity.SysUser;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.SysUserMapper;
import com.campus.activity.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 鉴权 Service 实现
 *
 * <p>登录规则 (对应需求文档 接口 1):
 * <ol>
 *   <li>账号不存在 / 密码错 → 1001 "账号或密码错误"</li>
 *   <li>status=0 → 1002 "账号已停用"</li>
 *   <li>密码用 BCryptPasswordEncoder.matches() 校验, 禁止明文存储与比对</li>
 * </ol>
 *
 * @author 勇闯天涯组
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 查询账号
        SysUser user = sysUserMapper.selectById(dto.getAccount());
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_OR_PASSWORD_ERROR);
        }

        // 2. 校验密码 (BCrypt.matches 明文 vs 加密值)
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.ACCOUNT_OR_PASSWORD_ERROR);
        }

        // 3. 校验账号状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 4. 签发 token
        String token = jwtUtil.generate(user.getAccount(), user.getRole(), user.getName());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setAccount(user.getAccount());
        vo.setName(user.getName());
        vo.setRole(user.getRole());

        log.info("用户登录成功: account={}, role={}", user.getAccount(), user.getRole());
        return vo;
    }
}
