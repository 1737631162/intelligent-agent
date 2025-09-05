package com.changan.vbot.service.dto;

import com.changan.vbot.dal.mongo.entity.AgentChatConversationDO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HomeAgentIntentionRecognitionResultDTO extends AgentIntentionRecognitionResultDTO {
    private List<AgentChatConversationDO> historyMessages;
    private String rewriteContent;
}
