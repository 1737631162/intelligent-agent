package com.changan.vbot.web.controller;

import com.changan.carbond.result.Msg;
import com.changan.vbot.service.AgentHuHomeControlService;
import com.changan.vbot.service.dto.AgentHuHomeControlReqDTO;
import com.changan.vbot.service.dto.AgentHuHomeControlRspDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/inner-api/v1/hu")
@Api(tags = "车家智联-车机控制智能家居")
public class AgentHuController {

    @Resource
    private AgentHuHomeControlService agentHuHomeControlService;

    @PostMapping("/control")
    @ApiOperation("家居设备语义控制接口")
    public Msg<AgentHuHomeControlRspDTO> homeControl(@Validated @RequestBody AgentHuHomeControlReqDTO controlReqDTO) {
        return agentHuHomeControlService.homeControl(controlReqDTO);
    }

}
