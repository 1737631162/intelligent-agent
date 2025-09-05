package com.changan.vbot.service.dto;

import lombok.Data;

import java.util.List;

@Data
public class DeviceMessageDataDTO {
    // 设备
    private String deviceId;
    private String deviceName;
    private String deviceCategory;
    private String categoryCode;
    private String brandName;
    private String brandCode;
    private List<String> tags;
    // 场景
    private String sceneId;
    private String sceneName;
}
