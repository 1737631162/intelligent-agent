package com.changan.vbot.common.openai;

import lombok.Data;

@Data
public class OpenAIToolCallDTO {
    private String id;
    private String type;
    private Integer index;
    private OpenAIToolCallFunctionDTO function;
}
