package com.changan.vbot.service;

import com.changan.carbond.result.Msg;
import com.changan.vbot.service.dto.AgentHuHomeControlReqDTO;
import com.changan.vbot.service.dto.AgentHuHomeControlRspDTO;

public interface AgentHuHomeControlService {
    Msg<AgentHuHomeControlRspDTO> homeControl(AgentHuHomeControlReqDTO controlReqDTO);
}
