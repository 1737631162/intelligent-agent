package com.changan.vbot.service.dto;

import lombok.Data;

@Data
public class AgentChatHomeControlResultDTO extends AgentChatResultDTO {
    private String event;
    private Boolean isControl;

    public static AgentChatHomeControlResultDTO ofDefault(String conversationId, String messageId) {
        AgentChatHomeControlResultDTO homeControlResultDTO = new AgentChatHomeControlResultDTO();
        homeControlResultDTO.setMessageId(messageId);
        homeControlResultDTO.setType(4);
        homeControlResultDTO.setConversationId(conversationId);
        homeControlResultDTO.setContent("");
        homeControlResultDTO.setAnswerType(0);
        return homeControlResultDTO;
    }

    public AgentChatHomeControlResultDTO content(String content) {
        super.setContent(content);
        return this;
    }

    public AgentChatHomeControlResultDTO event(String event) {
        this.event = event;
        return this;
    }
}
