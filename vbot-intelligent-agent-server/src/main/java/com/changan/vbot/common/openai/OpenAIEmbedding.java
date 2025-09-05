package com.changan.vbot.common.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OpenAIEmbedding {

    @JsonProperty("index")
    private Integer index;
    @JsonProperty("embedding")
    private List<Float> embedding;
    @JsonProperty("object")
    private String object;

}
