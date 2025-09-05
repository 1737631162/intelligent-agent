package com.changan.vbot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@AllArgsConstructor
@Getter
public enum HomeControlActionEnum {
    OPEN("open"),
    CLOSE("close"),
    SETTING("setting"),
    PAUSE("pause"),
    RESUME("resume"),
    CHARGE("charge"),
    QUERY("query"),
    UNKNOWN("unknown");

    private String action;

    public static HomeControlActionEnum of(String action) {
        if (Objects.isNull(action)) {
            return UNKNOWN;
        }
        for (HomeControlActionEnum value : HomeControlActionEnum.values()) {
            if (value.action.equals(action)) {
                return value;
            }
        }
        return UNKNOWN;
    }
}
