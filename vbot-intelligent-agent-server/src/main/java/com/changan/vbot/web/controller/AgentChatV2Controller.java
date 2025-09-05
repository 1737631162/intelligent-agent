package com.changan.vbot.web.controller;

import com.changan.carbond.ErrorCode;
import com.changan.carbond.result.Msg;
import com.changan.vbot.common.nebula.UserContextUtil;
import com.changan.vbot.service.IAgentChatService;
import com.changan.vbot.service.dto.AgentChatMessageDTO;
import com.changan.vbot.service.dto.AgentChatResultDTO;
import com.changan.vbot.web.response.AgentChatResponseVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Objects;

@Slf4j
@Api(tags = "智能体对话接口V2")
@RestController
@RequestMapping("/inner-api/v2/agent-chat")
public class AgentChatV2Controller {

    @Resource
    private IAgentChatService agentChatService;

    @PostMapping(value = "/chat")
    @ApiOperation("对话接口(管理接口)")
    public Msg<AgentChatResultDTO> chat(@RequestBody @Validated AgentChatMessageDTO chatMessage) throws Exception {
        if (Objects.isNull(chatMessage.getUserId())) {
            chatMessage.setUserId(UserContextUtil.queryUserId());
        }
        return agentChatService.chatV2(chatMessage);
    }

    @PostMapping(value = "/stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "流式对话接口(管理接口)", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<? extends AgentChatResultDTO> streamChat(@RequestBody @Validated AgentChatMessageDTO chatMessage) throws Exception {
        if (Objects.isNull(chatMessage.getUserId())) {
            chatMessage.setUserId(UserContextUtil.queryUserId());
        }
        chatMessage.setOpenId(UserContextUtil.queryOpenId());
        return agentChatService.streamChat(chatMessage);
    }

    @GetMapping("/query-conversation")
    @ApiOperation("对话记录查询接口")
    public Msg<List<AgentChatResponseVO>> queryConversation(@RequestParam("conversationId") String conversationId,
                                                            @RequestParam("content") String content,
                                                            @RequestParam(value = "userId", required = false) String userId,
                                                            @RequestParam("agentId") String agentId,
                                                            @RequestParam("carId") String carId) {
        if (Objects.isNull(userId)) {
            userId = UserContextUtil.queryUserId();
        }
        return agentChatService.queryConversation(conversationId, content, userId, agentId, carId);
    }

    @GetMapping("/delete-conversation")
    @ApiOperation("清空会话记录接口")
    public Msg<String> deleteConversation(@RequestParam(value = "userId", required = false) String userId,
                                          @NotBlank(message = "智能体id不能为空") @RequestParam("agentId") String agentId,
                                          @NotBlank(message = "车辆id不能为空") @RequestParam("carId") String carId) {
        if (Objects.isNull(userId)) {
            userId = UserContextUtil.queryUserId();
        }
        if (Objects.isNull(userId)) {
            return Msg.<String>error(ErrorCode.USER.USER_NOT_EXISTS).build();
        }
        return agentChatService.deleteConversation(userId, agentId, carId);
    }
}
