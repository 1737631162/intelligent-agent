package com.changan.vbot.common.openai;

import lombok.Data;

@Data
public class OpenAIResponseBaseDTO {

    private String id;
    private String object;
    private Long created;
    private String model;
    private Object usage;

}
