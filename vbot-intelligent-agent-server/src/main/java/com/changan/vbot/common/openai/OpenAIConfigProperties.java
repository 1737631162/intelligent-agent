package com.changan.vbot.common.openai;

import lombok.Data;

@Data
public class OpenAIConfigProperties {
    private String apiKey = "";
    private String baseUrl;
    private String completionsApi = "/v1/chat/completions";
    private String model;
    private String systemPrompt;
    private String toolPath;
    private Float temperature = 0.7F;
    private Integer maxNewTokens = 5000;
    private Float topP;
    private Integer topK;
    private Float presencePenalty;
    private Float frequencyPenalty;
}
