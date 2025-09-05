package com.changan.vbot.common.openai;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OpenAIToolCallDataDTO<R,S> {
    private String callId;
    private String function;
    private Boolean isError;
    private List<R> reqData;
    private S rspData;
}
