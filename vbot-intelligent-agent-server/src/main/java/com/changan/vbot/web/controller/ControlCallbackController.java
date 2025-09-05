package com.changan.vbot.web.controller;

import com.changan.carbond.result.Msg;
import com.changan.vbot.service.IControlCallbackService;
import com.changan.vbot.service.IOpenPlatService;
import com.changan.vbot.web.request.AgentChatRequestVO;
import com.changan.vbot.web.request.SmartTripCmdCallbackReqVO;
import com.changan.vbot.web.request.TaskSendCallbackVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 指令执行回调
 *
 * @Author: 赵建涛
 * @Date: 2024/03/28 14:33
 */
@Slf4j
@Api(tags = "机器人指令下发-指令执行回调")
@RestController
@RequestMapping("/inner-api/v1/control")
public class ControlCallbackController {

    @Autowired
    private IControlCallbackService controlCallbackService;

    @Autowired
    private IOpenPlatService openPlatService;

    @PostMapping("/send-control")
    @ApiOperation("前端指令下发")
    public Msg<String> sendControl(@RequestBody AgentChatRequestVO requestVO) {
        return Msg.success(openPlatService.sendControl(requestVO)).build();
    }

    @PostMapping("/callback")
    @ApiOperation("回调")
    public void callback(@RequestBody SmartTripCmdCallbackReqVO reqVO){
        controlCallbackService.callback(reqVO);
    }

    @PostMapping("/task-send-callback")
    @ApiOperation("任务下发回调")
    public void taskSendCallback(@RequestBody TaskSendCallbackVO callbackVO){
        controlCallbackService.taskSendCallback(callbackVO);
    }
}
