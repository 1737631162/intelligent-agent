package com.changan.vbot.common.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAICompletionRequestDTO {
    private List<ChatMessageDTO> messages;
    private String model;
    private Float temperature;
    @JsonProperty("max_new_tokens")
    private Integer maxNewTokens;
    @JsonProperty("top_p")
    private Float topP;
    @JsonProperty("top_k")
    private Integer topK;
    @JsonProperty("presence_penalty")
    private Float presencePenalty;
    @JsonProperty("frequency_penalty")
    private Float frequencyPenalty;
    private Boolean stream = false;
    private List<Object> tools;
    @JsonProperty("tool_choice")
    private Object toolChoice;
}
