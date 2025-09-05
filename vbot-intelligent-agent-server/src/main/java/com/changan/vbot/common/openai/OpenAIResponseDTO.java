package com.changan.vbot.common.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OpenAIResponseDTO extends OpenAIResponseBaseDTO {

    private List<OpenAIChoiceDTO> choices;
    @JsonProperty("prompt_logprobs")
    private String promptLogprobs;

}
