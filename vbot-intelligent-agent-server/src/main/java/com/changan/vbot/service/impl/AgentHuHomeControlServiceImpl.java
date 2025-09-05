package com.changan.vbot.service.impl;

import com.alibaba.fastjson2.JSON;
import com.changan.carbond.ErrorCode;
import com.changan.carbond.result.Msg;
import com.changan.vbot.common.configs.TuidAndOpenIdMappingConfig;
import com.changan.vbot.common.enums.AgentErrorCodeEnum;
import com.changan.vbot.common.enums.MessageRoleEnum;
import com.changan.vbot.common.openai.ChatMessageDTO;
import com.changan.vbot.common.openai.OpenAIChoiceDTO;
import com.changan.vbot.common.openai.OpenAIClient;
import com.changan.vbot.common.openai.OpenAICompletionRequestDTO;
import com.changan.vbot.common.openai.OpenAIResponseDTO;
import com.changan.vbot.common.openai.OpenAIToolCallDataDTO;
import com.changan.vbot.service.AgentHuHomeControlService;
import com.changan.vbot.service.IHomeDeviceService;
import com.changan.vbot.service.dto.AgentHuHomeControlReqDTO;
import com.changan.vbot.service.dto.AgentHuHomeControlRspDTO;
import com.changan.vbot.service.dto.HomeControlReqDTO;
import com.changan.vbot.service.dto.HomeControlRspDTO;
import com.changan.vbot.service.dto.VheResultDTO;
import com.changan.vbot.service.manage.IUserInfoService;
import com.changan.vbot.service.manage.feign.DeviceControlClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AgentHuHomeControlServiceImpl implements AgentHuHomeControlService {

    @Resource
    private IUserInfoService userInfoService;

    @Resource
    private IHomeDeviceService homeDeviceService;

    @Resource
    private DeviceControlClient deviceControlClient;

    @Resource(name = "extractOpenAIClient")
    private OpenAIClient extractOpenAIClient;

    @Resource
    private TuidAndOpenIdMappingConfig mappingConfig;

    @Override
    public Msg<AgentHuHomeControlRspDTO> homeControl(AgentHuHomeControlReqDTO controlReqDTO) {
        log.info("收到指令: {}", JSON.toJSONString(controlReqDTO));
        AgentHuHomeControlRspDTO controlRspDTO = new AgentHuHomeControlRspDTO();
        String tuid;
        try {
            tuid = this.decodeTuid(controlReqDTO.getTuidEncode());
        } catch (Exception e) {
            log.error("tuid解密失败", e);
            return Msg.<AgentHuHomeControlRspDTO>error(AgentErrorCodeEnum.INVALID_TUID).build();
        }
        log.info("解密后的tuid: {}", tuid);
//        String userId = userInfoService.queryCacIdByTuid(tuid); // 展车，演示，暂时注释掉
        String userId = mappingConfig.getOpenId(tuid);
        if (Objects.isNull(userId)) {
            return Msg.<AgentHuHomeControlRspDTO>error(ErrorCode.USER.USER_NOT_EXISTS).build();
        }
        List<ChatMessageDTO> extractMessages = new ArrayList<>();
        extractMessages.add(ChatMessageDTO.builder().role(MessageRoleEnum.USER.getCode()).content(controlReqDTO.getContent()).build());
        OpenAICompletionRequestDTO extractCompletionRequestDTO = OpenAICompletionRequestDTO.builder().messages(extractMessages).stream(false).build();
        Mono<OpenAIResponseDTO> responseDTOMono = extractOpenAIClient.call(extractCompletionRequestDTO);
        OpenAIResponseDTO responseDTO = responseDTOMono.block();
        log.info("TUID:{},车机输入信息：{}，参数抽取执行结果：{}", tuid, controlReqDTO.getContent(), JSON.toJSONString(responseDTO));
        OpenAIChoiceDTO choiceDTO = responseDTO.getChoices().get(0);
        ChatMessageDTO choiceMessageDTO = choiceDTO.getMessage();
        if (!ObjectUtils.isEmpty(choiceMessageDTO.getToolCalls())) {
            List<OpenAIToolCallDataDTO<HomeControlReqDTO, HomeControlRspDTO>> commands = homeDeviceService.convertToCommand(userId,controlReqDTO.getContent(), choiceMessageDTO.getToolCalls());
            // 只要有一条失败就全部失败
            List<OpenAIToolCallDataDTO<HomeControlReqDTO, HomeControlRspDTO>> fails = commands.stream().filter(OpenAIToolCallDataDTO::getIsError)
                    .collect(Collectors.toList());
            if (fails.isEmpty()) {
                List<HomeControlReqDTO> deviceCommands = commands.stream().flatMap(callDataDTO -> callDataDTO.getReqData().stream())
                        .collect(Collectors.toList());
                log.info("设备指令提交数据：{}", JSON.toJSONString(deviceCommands));
                VheResultDTO<List<String>> vehResultDTO = deviceControlClient.control(deviceCommands);
                log.info("设备指令提交结果：{}", JSON.toJSONString(vehResultDTO));
                if (vehResultDTO.getSuccess()) {
                    controlRspDTO.setTtsContent("您的智能家居操作指令已执行成功啦");
                }
            } else {
                controlRspDTO.setTtsContent("抱歉，我还没有学会这项技能，请您换个说法试一试");
            }
        } else {
            controlRspDTO.setTtsContent("您的车型还不支持此功能哦");
        }
        return Msg.success(controlRspDTO).build();
    }

    /**
     * tuid 解密
     *
     * @param tuidEncode
     * @return
     */
    private String decodeTuid(String tuidEncode) {
        String base64TuidReverse = new String(Base64.getDecoder().decode(tuidEncode));
        StringBuilder tuidReverse = new StringBuilder(base64TuidReverse).reverse();
        return new String(Base64.getDecoder().decode(tuidReverse.toString()));
    }
}
