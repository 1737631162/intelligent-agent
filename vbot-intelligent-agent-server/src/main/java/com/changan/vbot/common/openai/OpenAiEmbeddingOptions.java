package com.changan.vbot.common.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OpenAiEmbeddingOptions {
    private @JsonProperty("model")
    String model;
    private @JsonProperty("encoding_format")
    String encodingFormat;
    private @JsonProperty("dimensions")
    Integer dimensions;
    private @JsonProperty("user")
    String user;

}
