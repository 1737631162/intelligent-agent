package com.changan.vbot.web.controller;

import com.changan.carbond.result.Msg;
import com.changan.vbot.service.IAgentChatService;
import com.changan.vbot.web.request.AgentChatRequestVO;
import com.changan.vbot.web.response.AgentChatResponseVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@Api(tags = "智能体对话接口")
@RestController
@AllArgsConstructor
@RequestMapping("/inner-api/v1/chat")
public class AgentChatController {

    @Autowired
    private IAgentChatService agentChatService;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation("对话接口(管理接口)")
    public SseEmitter chat(@Validated @RequestParam("logId") String logId, @RequestParam("agentId") String agentId,
                           @RequestParam("question") String question, @RequestParam("source") Integer source,
                           @RequestParam("carId") String carId, @RequestParam("vin") String vin,
                           @RequestParam("userId") String userId) throws InterruptedException {

        AgentChatRequestVO chatReq = new AgentChatRequestVO();
        chatReq.setLogId(logId);
        chatReq.setAgentId(agentId);
        chatReq.setQuestion(question);
        chatReq.setSource(source);
        chatReq.setCarId(carId);
        chatReq.setVin(vin);
        chatReq.setUserId(userId);
        return agentChatService.chat(chatReq);
    }

    @GetMapping("/query-log")
    @ApiOperation("对话记录查询接口")
    public Msg<List<AgentChatResponseVO>> queryLog(@RequestParam("logId") String logId) {
        return agentChatService.queryLog(logId);
    }

}
