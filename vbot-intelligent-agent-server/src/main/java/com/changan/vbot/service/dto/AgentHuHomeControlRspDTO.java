package com.changan.vbot.service.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "车机家居设备控制返回实体")
public class AgentHuHomeControlRspDTO {
    @ApiModelProperty(value = "TTS播报内容", required = true)
    private String ttsContent;
}
