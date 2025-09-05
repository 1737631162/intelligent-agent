package com.changan.vbot.common.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OpenAIEmbeddingRequest extends OpenAiEmbeddingOptions {
    @JsonProperty("input")
    private List<String> input;
}
