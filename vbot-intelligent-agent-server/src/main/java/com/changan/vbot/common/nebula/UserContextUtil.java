package com.changan.vbot.common.nebula;


import com.alibaba.ttl.TransmittableThreadLocal;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * @author
 */
public class UserContextUtil {

    /**
     * @Fields contextHolder
     * TransmittableThreadLocal 能将主线程的本地数据在子线程创建的时候复制过去且可以解决线程池的问题，需要引入以下依赖。
     * 如果项目简单 可以使用上面的方式，无需引入依赖
     */
    private static final ThreadLocal<UserContext> contextHolder = new TransmittableThreadLocal<>();


    /**
     * 功能: 获取用户姓名
     * <p>
     * 参数: @return
     * 返回类型： String
     *
     * @throws
     * @since 1.0.0
     */
    public static String getCurrentUserName() {
        return getContext().getRealName();
    }

    public static String queryUser() {
        UserContext context = UserContextUtil.getContext();
        if (context != null && StringUtils.isNotBlank(context.getAccount())) {
            return context.getRealName() + "-" + context.getAccount();
        }
        return "system";
    }

    public static String queryUserId() {
        UserContext context = UserContextUtil.getContext();
        if (Objects.nonNull(context) && StringUtils.isNotBlank(context.getUserId())) {
            return context.getUserId();
        }
        return null;
    }

    public static String queryOpenId() {
        UserContext context = UserContextUtil.getContext();
        if (Objects.nonNull(context) && StringUtils.isNotBlank(context.getUserId())) {
            return context.getOpenId();
        }
        return "";
    }

    /**
     * 功能: 获取用户账号
     * <p>
     * 参数: @return
     * 返回类型： String
     *
     * @throws
     * @since 1.0.0
     */
    public static String getCurrentAccount() {
        return getContext().getAccount();
    }

    /**
     * 功能: 获取用户手机号
     * <p>
     * 参数: @return
     * 返回类型： String
     *
     * @throws
     * @since 1.0.0
     */
    public static String getCurrentPhone() {
        return getContext().getPhone();
    }

    /**
     * 功能: 获取用户当前租户
     * <p>
     * 参数: @return
     * 返回类型： String
     *
     * @throws
     * @since 1.0.0
     */
    public static String getCurrentTenantId() {
        return getContext().getCurrentTenantId();
    }

    public static void clearContext() {
        contextHolder.remove();
    }

    public static UserContext getContext() {
        return contextHolder.get();
    }

    public static void setContext(UserContext context) {
        Assert.notNull(context, "Only non-null UserContext instances are permitted");
        contextHolder.set(context);
    }

    public static UserContext createEmptyContext() {
        return new UserContext();
    }

}
