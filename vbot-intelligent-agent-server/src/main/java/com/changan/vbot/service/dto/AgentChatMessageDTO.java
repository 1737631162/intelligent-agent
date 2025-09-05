package com.changan.vbot.service.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

@Data
@ApiModel(description = "智能体聊天消息体")
public class AgentChatMessageDTO {

    //触发源智能体ID
    @ApiModelProperty(value = "触发源智能体ID")
    private String triggerAgentId;

    //被调用智能体ID
    @ApiModelProperty(value = "被调用智能体ID", required = true)
    private String agentId;

    //渠道来源
    @ApiModelProperty(value = "渠道来源:2-启源APP；3-引力APP；4-TOP SPACE APP;10-深蓝APP", required = true)
    private String sourceChannel;

    //会话ID
    @ApiModelProperty(value = "会话ID")
    private String conversationId;

    //会话内容
    @ApiModelProperty(value = "会话内容", required = true)
    private String content;

    //车辆ID
    @ApiModelProperty(value = "车辆ID", required = true)
    private String carId;

    //用户ID
    @ApiModelProperty(value = "用户ID", required = false)
    private String userId;

    @ApiModelProperty(value = "开放平台userId", required = false)
    private String openId;

    //经度
    @ApiModelProperty(value = "设备经度")
    private String lat;

    //纬度
    @ApiModelProperty(value = "设备纬度")
    private String lng;

    //指令
    @ApiModelProperty(value = "指令名称")
    private String cmd;

    //指令参数
    @ApiModelProperty(value = "指令参数")
    private Map<String, Object> cmdParams;

    //消息id
    @ApiModelProperty(value = "消息ID")
    private String messageId;

    //车系编码
    @ApiModelProperty(value = "车系编码", required = true)
    private String carSeries;

}
