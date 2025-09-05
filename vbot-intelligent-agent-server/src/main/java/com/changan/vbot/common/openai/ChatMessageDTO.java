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
public class ChatMessageDTO {
    private String role;
    @JsonProperty("reasoning_content")
    private String reasoningContent;
    private String content;
    // tool 消息需要使用
    private String name;
    @JsonProperty("tool_call_id")
    private String toolCallId;
    @JsonProperty("tool_calls")
    private List<OpenAIToolCallDTO> toolCalls;
}
