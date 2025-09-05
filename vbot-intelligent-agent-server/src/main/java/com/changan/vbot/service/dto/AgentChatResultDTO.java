package com.changan.vbot.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public abstract class AgentChatResultDTO {
    //会话ID
    @ApiModelProperty(value = "会话ID")
    private String conversationId;

    // 消息ID
    @ApiModelProperty(value = "消息ID")
    private String messageId;

    // 文内容
    @ApiModelProperty(value = "对话内容")
    private String content;

    // 内容格式 json,text,html,markdown
    @ApiModelProperty(value = "内容格式：json、text、html、markdown")
    private String contentFormat;

    // 问题类型(0-查询,1-问答,2-控车,3-备车,4-控家)
    @ApiModelProperty(value = "问题类型：0-查询,1-问答,2-控车,3-备车,4-控家")
    private Integer type;

    @ApiModelProperty(value = "是否结束")
    private Boolean isFinal;

    @ApiModelProperty(value = "是否来自自己")
    private Boolean isFromSelf;

    @ApiModelProperty(value = "大模型回复类型：reply、recommended")
    private String llmType;

    @ApiModelProperty(value = "是否敏感")
    private Boolean isEvil;

    @ApiModelProperty(value = "推荐问题")
    private List<String> recommendeds;

    // 1-正常结束 2-异常结束 3-超时结束
    @ApiModelProperty(value = "结束类型 1-正常结束 2-异常结束 3-超时结束")
    private Integer answerStatus;

    @ApiModelProperty(value = "答案类型 0-文本 1-图片 2-表格")
    private Integer answerType;

}
