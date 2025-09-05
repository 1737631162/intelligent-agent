package com.changan.vbot.common.openai;

import lombok.Data;

import java.util.List;

@Data
public class OpenAIEmbeddingResponseDTO extends OpenAIResponseBaseDTO {

    private List<OpenAIEmbedding> data;

}
