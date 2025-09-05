package com.changan.vbot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ChatMessageEventEnum {
    STEP("step"),
    REPLY("reply"),
    ERROR("error")
    ;

    String code;
}
