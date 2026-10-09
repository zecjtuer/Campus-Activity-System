package com.campus.activity.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * <p>1. 分页插件: 统一分页使用 MyBatis-Plus Page</p>
 * <p>2. 全局逻辑删除字段 (deleted): 由 application.yml 中
 *    mybatis-plus.global-config.db-config.logic-delete-field 控制,
 *    实体上 @TableLogic 注解兜底。</p>
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册分页插件, 拦截所有 SELECT 进行分页
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
