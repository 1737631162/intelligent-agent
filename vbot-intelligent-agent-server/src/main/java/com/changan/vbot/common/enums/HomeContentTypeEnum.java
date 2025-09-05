package com.changan.vbot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum HomeContentTypeEnum {
    NAME(0),
    TAG(1),
    CATEGORY(2),
    BRAND(3),
    ;

    private Integer code;

}
