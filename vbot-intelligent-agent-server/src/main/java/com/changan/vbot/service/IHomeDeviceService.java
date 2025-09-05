package com.changan.vbot.service;

import com.changan.vbot.common.openai.OpenAIToolCallDTO;
import com.changan.vbot.common.openai.OpenAIToolCallDataDTO;
import com.changan.vbot.service.dto.DeviceMessageDTO;
import com.changan.vbot.service.dto.HomeControlReqDTO;
import com.changan.vbot.service.dto.HomeControlRspDTO;

import java.util.List;

public interface IHomeDeviceService {
    void syncDeviceInfo(DeviceMessageDTO messageDTO);

    void syncSceneInfo(DeviceMessageDTO messageDTO);

    /**
     * 函数转换成可执行指令，添加设备信息
     *
     * @param toolCalls
     */
    List<OpenAIToolCallDataDTO<HomeControlReqDTO, HomeControlRspDTO>> convertToCommand(String userId,String command, List<OpenAIToolCallDTO> toolCalls);

    void syncBrandInfo(DeviceMessageDTO messageDTO);
}
