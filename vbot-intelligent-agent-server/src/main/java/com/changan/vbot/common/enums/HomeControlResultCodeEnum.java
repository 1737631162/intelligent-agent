package com.changan.vbot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum HomeControlResultCodeEnum {
    RUNNING("0", "执行中"),
    SUCCESS("1", "成功"),
    FAIL("2", "失败"),
    ;

    private String code;
    private String msg;
}
