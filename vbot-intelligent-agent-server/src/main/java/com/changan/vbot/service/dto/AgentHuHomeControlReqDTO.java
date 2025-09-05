package com.changan.vbot.service.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(description = "家居控制请求实体")
public class AgentHuHomeControlReqDTO {

    @ApiModelProperty(value = "请求ID", required = true)
    @NotBlank(message = "请求ID不能为空")
    private String requestId;

    @ApiModelProperty(value = "控制指令", required = true)
    @NotBlank(message = "控制指令不能为空")
    private String content;

    @ApiModelProperty(value = "加密tuid", required = true)
    @NotBlank(message = "加密tuid不能为空")
    private String tuidEncode;

}
