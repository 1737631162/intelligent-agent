package com.changan.vbot.common.nebula;

import lombok.Data;

import java.util.Collection;

@Data
public class UserContext {

    private String userId;

    private String account;

    /**
     * @Fields realName 用户姓名
     */
    private String realName;

    /**
     * @Fields phone 手机号
     */
    private String phone;

    /**
     * @Fields email 邮箱
     */
    private String email;

    /**
     * @Fields cmpToken
     */
    private String cmpToken;

    /**
     * @Fields currentTenantId 用户当前选择的租户
     */
    private String currentTenantId;

    /**
     * @Fields currentTenantConfigType 当前选择租户对应的配置类别
     */
    private String currentTenantConfigType;

    /**
     * 用户类型;0-B端员工，1-C端员工
     */
    private String userType;


    /**
     * @Fields providerId 供应商id
     */
    private String providerId;

    private String tokenKey;

    public Collection<String> authorities;

    private String traceId;

    private String openId;
}
