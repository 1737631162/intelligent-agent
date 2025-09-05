package com.changan.vbot.common.openai;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Data
@Slf4j
public class OpenAIToolCallFunctionDTO {
    private String name;
    private String arguments;
    @JsonIgnore
    private JSONObject jsonArguments;

    public void setArguments(String arguments) {
        this.arguments = arguments;
        if (Objects.nonNull(arguments)) {
            try {
                this.jsonArguments = JSONObject.parseObject(arguments);
            } catch (Exception e) {
                this.jsonArguments = new JSONObject();
                log.warn("解析参数失败：{}", arguments);
            }
        }
    }
}
