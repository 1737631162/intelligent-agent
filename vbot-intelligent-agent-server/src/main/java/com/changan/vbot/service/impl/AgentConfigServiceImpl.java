package com.changan.vbot.service.impl;

import com.changan.carbond.result.Msg;
import com.changan.vbot.service.IAgentConfigService;
import org.springframework.stereotype.Service;

@Service
public class AgentConfigServiceImpl implements IAgentConfigService {


    @Override
    public Msg<?> getAgentConfig(String agentId, String sourceChannel) {
        return null;
    }
}
