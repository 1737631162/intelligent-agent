package com.changan.vbot.service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class HomeControlReqDTO {
    private String commandId; // 指令ID
    private String action; // 控制动作 @see HomeControlActionEnum
    private String id; // 设备id或场景id
    private String userId; // 用户id
    private Integer type; // 控制类型 0-设备 1-场景
    private Map<String, Object> params; // 设置参数
}
