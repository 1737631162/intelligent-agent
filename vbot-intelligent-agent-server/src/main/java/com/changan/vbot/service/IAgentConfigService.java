package com.changan.vbot.service;

import com.changan.carbond.result.Msg;

public interface IAgentConfigService {

    Msg<?> getAgentConfig(String agentId,String sourceChannel);

}
