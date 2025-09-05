package com.changan.vbot.common.filter;

import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.changan.vbot.common.nebula.UserContext;
import com.changan.vbot.common.nebula.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Order(1)
@WebFilter(urlPatterns = "/admin/inner-api/*")
public class AdminRequestUserFilter extends OncePerRequestFilter {

    private static final List<String> IGNORE_URLS = new ArrayList<>();
    private static final String CA_AUTH_HEADER = "CA_Authorization";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Map<String, Object> fail = new HashMap<String, Object>();
        fail.put("code", 403);
        fail.put("msg", "资源未授权！");
        fail.put("success", false);
        // 判断接口是否要过滤 业务系统可以根据需求是否需要此判断，无需判断时则将相关代码可以移除
        String uri = request.getRequestURI();
        if (checkUrl(uri)) {
            String caAuthRequest = request.getHeader(CA_AUTH_HEADER);
            String decodeAuthRequest = Base64.decodeStr(caAuthRequest, "UTF-8");
            log.info("request user info:{}", decodeAuthRequest);
            if (StringUtils.isBlank(decodeAuthRequest)) {
                log.error("未授权请求！");
                returnJson(response, fail);
                return;
            }
            JSONObject userObject = JSON.parseObject(decodeAuthRequest);
            UserContext userContext = new UserContext();
            userContext.setAccount(userObject.getString("account"));
            userContext.setCmpToken(userObject.getString("cmpToken"));
            userContext.setCurrentTenantId(userObject.getString("currentTenantId"));
            userContext.setCurrentTenantConfigType(userObject.getString("currentTenantConfigType"));
            userContext.setEmail(userObject.getString("email"));
            userContext.setPhone(userObject.getString("phone"));
            userContext.setRealName(userObject.getString("realName"));
            userContext.setUserId(userObject.getString("userId"));
            userContext.setUserType(userObject.getString("userType"));
            userContext.setTokenKey(userObject.getString("tokenKey"));
            userContext.setProviderId(userObject.getString("providerId"));
            if (StringUtils.isBlank(userContext.getAccount()) || StringUtils.isBlank(userContext.getCurrentTenantId())) {
                log.error("未授权请求！");
                returnJson(response, fail);
                return;
            }
            UserContextUtil.setContext(userContext);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContextUtil.clearContext();
        }
    }

    private boolean checkUrl(String requestURI) {
        for (String url : IGNORE_URLS) {
            if (requestURI.contains(url)) {
                return false;
            }
        }
        return true;
    }

    private void returnJson(ServletResponse response, Map rep) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        try (PrintWriter writer = response.getWriter()) {
            writer.print(JSON.toJSONString(rep));

        } catch (IOException e) {
            log.error("response error", e);
        }
    }
}
