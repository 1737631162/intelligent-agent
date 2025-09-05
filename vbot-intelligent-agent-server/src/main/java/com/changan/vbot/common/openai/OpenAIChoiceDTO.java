package com.changan.vbot.common.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OpenAIChoiceDTO {
    private Integer index;
    private ChatMessageDTO delta;
    private ChatMessageDTO message;
    private String logprobs;
    @JsonProperty("finish_reason")
    private String finishReason;
    @JsonProperty("stop_reason")
    private String stopReason;
}
