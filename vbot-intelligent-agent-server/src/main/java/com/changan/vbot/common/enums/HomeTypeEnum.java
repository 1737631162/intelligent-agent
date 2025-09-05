package com.changan.vbot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum HomeTypeEnum {
    DEVICE(0, "设备"),
    SCENE(1, "场景");
    private Integer code;
    private String name;
}
