package com.changan.vbot.common.enums;

import com.changan.carbond.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AgentErrorCodeEnum implements IErrorCode {
    SUCCESS(0, "操作成功"),
    UNAUTHORIZED(401, "未授权"),
    SOURCE_NOT_FOUND(404, "资源不存在"),
    SYSTEM_EXCEPTION(40001, "系统异常"),
    MISSING_PARAM_IS(40005, "缺少必填参数-%s"),
    INVALID_TUID(40006, "tuid非法"),
    NO_MATCH(500001, "没有匹配项"),
    TENCENT_LKE_ERROR(500100, "腾讯知识引擎异常"),
    COZE_AGENT_ERROR(500200, "扣子智能体异常"),
    AGENT_NOT_SUPPORT_NON_STREAM(500300, "该智能体不支持非流式处理"),
    LLM_SERVER_ERROR(500301, "大模型服务不可用"),
    MILVUS_CLIENT_ERROR(500302, "Milvus客户端异常"),
    ;


    private int code;
    private String msg;

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.msg;
    }

}
