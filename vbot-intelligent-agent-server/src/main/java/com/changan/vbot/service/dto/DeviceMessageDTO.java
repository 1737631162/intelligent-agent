package com.changan.vbot.service.dto;

import lombok.Data;

@Data
public class DeviceMessageDTO {
    private Integer event; // 0-新增 1-修改 2-删除
    private Integer type; // 0-设备 1-场景 2-品牌
    private String userId;
    private DeviceMessageDataDTO data;
}

