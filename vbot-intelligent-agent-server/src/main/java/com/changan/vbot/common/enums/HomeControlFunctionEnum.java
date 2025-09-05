package com.changan.vbot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum HomeControlFunctionEnum {
    AIR_CONDITION_CONTROL("air_condition_control", "WallAC", "空调"),
    FAN_DEVICE_CONTROL("fan_device_control", "Fan", "风扇"),
    TV_DEVICE_CONTROL("tv_device_control", "Television", "电视"),
    DOOR_LOCK_CONTROL("door_lock_control", "DoorLock", "门锁"),
    DEHUMIDIFIER_CONTROL("dehumidifier_control", "Dehumidifier", "除湿机"),
    AIR_PURIFIER_CONTROL("air_purifier_control", "AirPurifier", "空气净化器"),
    CURTAIN_DEVICE_CONTROL("curtain_device_control", "ElectricCurtain", "窗帘"),
    LIGHT_DEVICE_CONTROL("light_device_control", "SmartLight", "灯"),
    SWEEPER_DEVICE_CONTROL("sweeper_device_control", "Sweeper", "扫地机"),
    SCENE_MODE_CONTROL("scene_mode_control", "Scene", "模式"),
    UNKNOWN_DEVICE_CONTROL("unknown_device_control", "UnknownDevice", "未知设备控制"),
    UNKNOWN_FUNCTION("unknown_function", "Unknown", "未知函数"),
    ;
    private String function;
    private String categoryCode;
    private String description;

    public static HomeControlFunctionEnum of(String function) {
        for (HomeControlFunctionEnum value : HomeControlFunctionEnum.values()) {
            if (value.function.equals(function)) {
                return value;
            }
        }
        return UNKNOWN_FUNCTION;
    }
}
