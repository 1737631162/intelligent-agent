package com.changan.vbot.common.filter;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import com.changan.carbond.ErrorCode;
import com.changan.carbond.basesserver.api.CoreUserApiFeignClient;
import com.changan.carbond.common.ApiResult;
import com.changan.carbond.dto.FullCoreUserDto;
import com.changan.carbond.result.Msg;
import com.changan.vbot.common.constants.CacheConstants;
import com.changan.vbot.common.enums.AgentErrorCodeEnum;
import com.changan.vbot.common.nebula.UserContext;
import com.changan.vbot.common.nebula.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Order(2)
@WebFilter(urlPatterns = {"/inner-api/v2/agent-chat/*"}, asyncSupported = true)
public class AppRequestUserFilter extends OncePerRequestFilter {

    private static final List<String> IGNORE_URLS = new ArrayList<String>();
    private static final String TRACE_ID = "Request-Id";
    private static final String APP_TYPE = "appType";
    private static final String DEVICE_ID = "deviceId";

    @Autowired
    private CoreUserApiFeignClient coreUserApiFeignClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Msg<Object> failMsg = Msg.error(AgentErrorCodeEnum.UNAUTHORIZED).build();
        log.info("请求url:{}", request.getRequestURI());
        String traceId = handleCommonHeader(request);
        String baseUserId = request.getParameter("userId");
        if (StringUtils.isEmpty(baseUserId)) {
            log.error("未授权请求！");
            returnJson(response, failMsg);
            return;
        }
        if (checkUrl(request.getRequestURI())) {
            UserContext userContext = null;
            String redisUserInfoKey = CacheConstants.APP_USER_INFO_REDIS_KEY + baseUserId;
            // 得到用户信息
            String userInfoStr = stringRedisTemplate.opsForValue().get(redisUserInfoKey);
            if (StringUtils.isEmpty(userInfoStr)) {
                failMsg.setCode(ErrorCode.USER.USER_NOT_EXISTS.getCode());
                failMsg.setMsg(ErrorCode.USER.USER_NOT_EXISTS.getMessage());
                // 获取用户信息
                try {
                    ApiResult<FullCoreUserDto> baseUserResp = coreUserApiFeignClient.getFullUserInfoById(baseUserId);
                    if (!baseUserResp.success() || baseUserResp.getData() == null || StringUtils.isEmpty(baseUserResp.getData().getOpenId())) {
                        log.error("获取用户信息失败！{}", baseUserResp);
                        returnJson(response, failMsg);
                        return;
                    }
                    userContext = new UserContext();
                    BeanUtil.copyProperties(baseUserResp.getData(), userContext);
                    userContext.setOpenId(baseUserResp.getData().getOpenId());
                    stringRedisTemplate.opsForValue().set(redisUserInfoKey, JSON.toJSONString(userContext), CacheConstants.USER_INFO_TIMEOUT, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("获取用户信息异常！");
                    returnJson(response, failMsg);
                    return;
                } finally {
                    MDC.clear();
                }
            } else {
                userContext = JSON.parseObject(userInfoStr, UserContext.class);
            }
            userContext.setTraceId(traceId);
            userContext.setUserId(baseUserId);
            log.info("request user userContext:{}", JSON.toJSONString(userContext));
            UserContextUtil.setContext(userContext);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private boolean checkUrl(String requestURI) {
        if (IGNORE_URLS != null && !IGNORE_URLS.isEmpty()) {
            for (String url : IGNORE_URLS) {
                if (requestURI.endsWith(url)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String handleCommonHeader(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID);
        String appType = request.getHeader(APP_TYPE);
        String deviceId = request.getHeader(DEVICE_ID);
        MDC.put("TRACE_ID", traceId);
        MDC.put("DEVICE_ID", deviceId);
        MDC.put("APP_TYPE", appType);
        return traceId;
    }

    private void returnJson(ServletResponse response, Msg<Object> rep) {
        PrintWriter writer = null;
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        try {
            writer = response.getWriter();
            writer.print(JSON.toJSONString(rep));
        } catch (IOException e) {
            log.error("response error", e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
