package com.changan.vbot.common.openai;

import lombok.Data;

@Data
public class OpenAiEmbeddingConfigProperties {
    private String apiKey = "";
    private String baseUrl;
    private String embeddingApi = "/v1/embeddings";
    private String model;
    private String encodingFormat;
    private Integer dimensions;
}
