package com.changan.vbot.service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class HomeControlQueryReqDTO {
    private String commandId; // 指令ID
    private Integer type; // 控制类型 0-设备 1-场景
}
