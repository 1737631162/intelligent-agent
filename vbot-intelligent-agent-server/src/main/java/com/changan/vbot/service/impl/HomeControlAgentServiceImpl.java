package com.changan.vbot.service.impl;


import com.alibaba.fastjson2.JSON;
import com.changan.carbond.result.Msg;
import com.changan.vbot.common.enums.AgentErrorCodeEnum;
import com.changan.vbot.common.enums.AgentIdEnum;
import com.changan.vbot.common.enums.ChatMessageEventEnum;
import com.changan.vbot.common.enums.MessageRoleEnum;
import com.changan.vbot.common.openai.ChatMessageDTO;
import com.changan.vbot.common.openai.OpenAIChoiceDTO;
import com.changan.vbot.common.openai.OpenAIClient;
import com.changan.vbot.common.openai.OpenAICompletionRequestDTO;
import com.changan.vbot.common.openai.OpenAIResponseDTO;
import com.changan.vbot.common.openai.OpenAIToolCallDataDTO;
import com.changan.vbot.dal.mongo.entity.AgentChatConversationDO;
import com.changan.vbot.service.IAgentService;
import com.changan.vbot.service.IHomeDeviceService;
import com.changan.vbot.service.dto.AgentChatHomeControlResultDTO;
import com.changan.vbot.service.dto.AgentChatMessageDTO;
import com.changan.vbot.service.dto.AgentChatResultDTO;
import com.changan.vbot.service.dto.AgentIntentionRecognitionResultDTO;
import com.changan.vbot.service.dto.AgentPostChatHomeResultDTO;
import com.changan.vbot.service.dto.AgentPostChatResultDTO;
import com.changan.vbot.service.dto.AgentPreChatHomeControlResultDTO;
import com.changan.vbot.service.dto.AgentPreChatResultDTO;
import com.changan.vbot.service.dto.HomeAgentIntentionRecognitionResultDTO;
import com.changan.vbot.service.dto.HomeControlQueryReqDTO;
import com.changan.vbot.service.dto.HomeControlReqDTO;
import com.changan.vbot.service.dto.HomeControlRspDTO;
import com.changan.vbot.service.dto.VheResultDTO;
import com.changan.vbot.service.manage.feign.DeviceControlClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service("home_control_agent")
@Slf4j
public class HomeControlAgentServiceImpl implements IAgentService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource(name = "rewriteOpenAIClient")
    private OpenAIClient rewriteOpenAIClient;

    @Resource(name = "extractOpenAIClient")
    private OpenAIClient extractOpenAIClient;

    @Resource(name = "chatOpenAIClient")
    private OpenAIClient chatOpenAIClient;

    @Resource
    private IHomeDeviceService homeDeviceService;

    @Resource
    private DeviceControlClient deviceControlClient;

    @Value("${openai.rewrite.user-prompt}")
    private String rewriteUserPrompt;

    @Override
    public String getAgentId() {
        return AgentIdEnum.HOME.getAgentId();
    }

    @Override
    public String getAgentName() {
        return AgentIdEnum.HOME.getAgentName();
    }

    @Override
    public Msg<? extends AgentIntentionRecognitionResultDTO> intentionRecognition(AgentChatMessageDTO message) {
        // 获取会话信息
        Query query = Query.query(Criteria.where("conversationId").is(message.getConversationId()).and("agentId")
                .is(message.getAgentId()).and("userId").is(message.getUserId())).limit(8);
        query.with(Sort.by(Sort.Direction.DESC, "createAt"));
        List<AgentChatConversationDO> messages = mongoTemplate.find(query, AgentChatConversationDO.class);
        Collections.reverse(messages);
        HomeAgentIntentionRecognitionResultDTO recognitionResultDTO = HomeAgentIntentionRecognitionResultDTO.builder().historyMessages(messages).build();
        // 改写，获取会话信息，决定是否改写
        if (!ObjectUtils.isEmpty(messages)) {
            // 完善重写逻辑
            String historyMessages = messages.stream().filter(agentChatConversationDO -> agentChatConversationDO.getFrom() == 0).limit(4).map(AgentChatConversationDO::getContent).collect(Collectors.joining("\n"));
            String prompt = rewriteUserPrompt.replace("{historyMessages}", historyMessages).replace("{currentMessage}", message.getContent());
            List<ChatMessageDTO> rewriteMessages = new ArrayList<>();
            rewriteMessages.add(ChatMessageDTO.builder().role(MessageRoleEnum.USER.getCode()).content(prompt).build());
            OpenAICompletionRequestDTO completionRequestDTO = OpenAICompletionRequestDTO.builder().messages(rewriteMessages).stream(false).build();
            Mono<OpenAIResponseDTO> responseDTOMono = rewriteOpenAIClient.call(completionRequestDTO);
            OpenAIResponseDTO responseDTO = responseDTOMono.block();
            recognitionResultDTO.setRewriteContent(responseDTO.getChoices().get(0).getMessage().getContent());
            log.info("history content：{},current content:{},rewrite content:{}", historyMessages, message.getContent(), recognitionResultDTO.getRewriteContent());
        } else {
            recognitionResultDTO.setRewriteContent(message.getContent());
        }
        return Msg.success(recognitionResultDTO).build();
    }

    @Override
    public Msg<? extends AgentPreChatResultDTO> preChat(AgentChatMessageDTO message, AgentIntentionRecognitionResultDTO intentionRecognition) throws Exception {
        // 插入用户问题聊天记录数据
        HomeAgentIntentionRecognitionResultDTO intentionRecognitionResultDTO = (HomeAgentIntentionRecognitionResultDTO) intentionRecognition;
        AgentChatConversationDO conversationDO = AgentChatConversationDO.of(message);
        conversationDO.setConversationId(message.getConversationId());
        conversationDO.setUserId(message.getUserId());
        conversationDO.setContent(message.getContent());
        conversationDO.setFrom(0);
        conversationDO.setId(message.getMessageId());
        conversationDO.setAgentId(message.getAgentId());
        conversationDO.setTriggerAgentId(message.getTriggerAgentId());
        conversationDO.setCarId(message.getCarId());
        conversationDO.setSource(Integer.parseInt(message.getSourceChannel()));
        conversationDO.setType(3);
        conversationDO.setRewriteContent(intentionRecognitionResultDTO.getRewriteContent());
        mongoTemplate.insert(conversationDO);
        return Msg.success(new AgentPreChatHomeControlResultDTO()).build();
    }

    @Override
    public Msg<? extends AgentChatResultDTO> chat(AgentChatMessageDTO message, AgentIntentionRecognitionResultDTO intentionRecognition, AgentPreChatResultDTO preChat) throws Exception {
        return Msg.<AgentChatResultDTO>error(AgentErrorCodeEnum.AGENT_NOT_SUPPORT_NON_STREAM).build();
    }


    @Override
    public Flux<? extends AgentChatResultDTO> streamChat(AgentChatMessageDTO message, AgentIntentionRecognitionResultDTO intentionRecognition, AgentPreChatResultDTO preChat) throws Exception {
        HomeAgentIntentionRecognitionResultDTO recognition = (HomeAgentIntentionRecognitionResultDTO) intentionRecognition;
        // 构建历史对话
        List<AgentChatConversationDO> conversations = recognition.getHistoryMessages();
        List<ChatMessageDTO> chatMessages = new ArrayList<>();
        if (!ObjectUtils.isEmpty(conversations)) {
            chatMessages.addAll(conversations.stream().map(conversation -> {
                ChatMessageDTO chatMessageDTO = ChatMessageDTO.builder().content(conversation.getContent()).build();
                if (conversation.getFrom() == 0) {
                    chatMessageDTO.setRole(MessageRoleEnum.USER.getCode());
                } else {
                    chatMessageDTO.setRole(MessageRoleEnum.ASSISTANT.getCode());
                }
                return chatMessageDTO;
            }).collect(Collectors.toList()));
        }
        // 加入当前对话
        String messageId = UUID.randomUUID().toString();
        chatMessages.add(ChatMessageDTO.builder().content(message.getContent()).role(MessageRoleEnum.USER.getCode()).build());
        // 抽取调用抽参模型获取设备控制信息
        return this.homeControl(messageId, message, recognition, chatMessages, preChat);
    }

    private Flux<? extends AgentChatResultDTO> homeControl(String messageId, AgentChatMessageDTO message, HomeAgentIntentionRecognitionResultDTO recognition, List<ChatMessageDTO> chatMessages, AgentPreChatResultDTO preChat) {
        AgentChatHomeControlResultDTO homeControlResultDTO = AgentChatHomeControlResultDTO.ofDefault(message.getConversationId(), messageId).event(ChatMessageEventEnum.REPLY.getCode());
        Flux<AgentChatHomeControlResultDTO> controlResultDTOFlux = Flux.create(sink -> {
            // STEP 信息抽取
            sink.next(AgentChatHomeControlResultDTO.ofDefault(message.getConversationId(), messageId).content("信息抽取").event(ChatMessageEventEnum.STEP.getCode()));
            List<ChatMessageDTO> extractMessages = new ArrayList<>();
            extractMessages.add(ChatMessageDTO.builder().role(MessageRoleEnum.USER.getCode()).content(recognition.getRewriteContent()).build());
            OpenAICompletionRequestDTO extractCompletionRequestDTO = OpenAICompletionRequestDTO.builder().messages(extractMessages).stream(false).build();
            Mono<OpenAIResponseDTO> responseDTOMono = extractOpenAIClient.call(extractCompletionRequestDTO);
            OpenAIResponseDTO responseDTO = responseDTOMono.block();
            log.info("输入信息：{}，参数抽取执行结果：{}", recognition.getRewriteContent(), JSON.toJSONString(responseDTO));
            OpenAIChoiceDTO choiceDTO = responseDTO.getChoices().get(0);
            ChatMessageDTO choiceMessageDTO = choiceDTO.getMessage();
            AtomicBoolean isControl = new AtomicBoolean(false);
            if (!ObjectUtils.isEmpty(choiceMessageDTO.getToolCalls())) {
                // 将工具输出加入chatMessages
                chatMessages.add(choiceMessageDTO);
                // 执行设备指令，获取设备指令执行结果
                // STEP 获取设备
                sink.next(AgentChatHomeControlResultDTO.ofDefault(message.getConversationId(), messageId).content("获取设备").event(ChatMessageEventEnum.STEP.getCode()));
                List<OpenAIToolCallDataDTO<HomeControlReqDTO, HomeControlRspDTO>> commands;
                try {
                    commands = homeDeviceService.convertToCommand(message.getOpenId(), recognition.getRewriteContent(), choiceMessageDTO.getToolCalls());
                } catch (Exception e) {
                    sink.next(AgentChatHomeControlResultDTO.ofDefault(message.getConversationId(), messageId).content("获取设备失败").event(ChatMessageEventEnum.ERROR.getCode()));
                    sink.complete();
                    return;
                }
                log.info("设备指令转换结果：{}", JSON.toJSONString(commands));
                // STEP 执行指令
                sink.next(AgentChatHomeControlResultDTO.ofDefault(message.getConversationId(), messageId).content("执行指令").event(ChatMessageEventEnum.STEP.getCode()));
                List<HomeControlReqDTO> deviceCommands = commands.stream().filter(command -> !command.getIsError())
                        .flatMap(command -> command.getReqData().stream())
                        .collect(Collectors.toList());
                log.info("设备指令提交数据：{}", JSON.toJSONString(deviceCommands));
                VheResultDTO<List<String>> vehResultDTO = deviceControlClient.control(deviceCommands);
                log.info("设备指令提交结果：{}", JSON.toJSONString(vehResultDTO));
                if (vehResultDTO.getSuccess()) {
                    // STEP 获取结果
                    sink.next(AgentChatHomeControlResultDTO.ofDefault(message.getConversationId(), messageId).content("获取结果").event(ChatMessageEventEnum.STEP.getCode()));
                    // 获取设备指令执行结果
                    // 这里需要通过轮寻来获取结果，200毫秒轮寻一次，最多等待3秒，最后需要执行结果加入chatMessages
                    int count = 0;
                    long start = System.currentTimeMillis();
                    Map<String, HomeControlRspDTO> commandResults = new HashMap<>();
                    while (System.currentTimeMillis() - start <= 3000) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(200);
                        } catch (InterruptedException e) {
                            log.warn("等待获取指令执行结果被中断", e);
                        }
                        List<HomeControlQueryReqDTO> queryCommands = deviceCommands.stream()
                                .filter(command -> !commandResults.containsKey(command.getCommandId()))
                                .map(command -> HomeControlQueryReqDTO.builder()
                                        .commandId(command.getCommandId()).type(command.getType()).build()).collect(Collectors.toList());
                        if (queryCommands.isEmpty()) {
                            break;
                        }
                        VheResultDTO<List<HomeControlRspDTO>> queryResult = deviceControlClient.queryControlResult(queryCommands);
                        log.info("第{}次请求设备指令执行结果：{}", ++count, JSON.toJSONString(queryResult));
                        if (queryResult.getSuccess()) {
                            List<HomeControlRspDTO> queryRsps = queryResult.getData();
                            if (Objects.nonNull(queryRsps)) {
                                queryRsps.forEach(queryRsp -> {
                                    if (queryRsp.isFinished()) {
                                        commandResults.put(queryRsp.getCommandId(), queryRsp);
                                    }
                                });
                            }
                        }
                    }
                    // 错误指令加入直接加入chatMessages，正确指令根据执行结果加入 chatMessages
                    commands.forEach(command -> {
                        if (command.getIsError()) {
                            chatMessages.add(ChatMessageDTO.builder()
                                    .toolCallId(command.getCallId())
                                    .name(command.getFunction())
                                    .role(MessageRoleEnum.TOOL.getCode())
                                    .content(JSON.toJSONString(Collections.singletonList(command.getRspData())))
                                    .build());
                        } else {
                            List<HomeControlRspDTO> functionResults = new LinkedList<>();
                            command.getReqData().forEach(reqData -> {
                                // cong commandResults 获取结果，获取不到，就认为是超时没响应的
                                HomeControlRspDTO rspData = commandResults.get(reqData.getCommandId());
                                if (Objects.nonNull(rspData)) {
                                    functionResults.add(rspData);
                                } else {
                                    functionResults.add(HomeControlRspDTO.builder()
                                            .commandId(reqData.getCommandId())
                                            .errorMsg("指令已下发，设备响应超时")
                                            .resultCode("0")
                                            .data(reqData)
                                            .build());
                                }
                            });
                            chatMessages.add(ChatMessageDTO.builder()
                                    .toolCallId(command.getCallId())
                                    .name(command.getFunction())
                                    .content(JSON.toJSONString(functionResults))
                                    .role(MessageRoleEnum.TOOL.getCode())
                                    .build());
                        }
                    });
                } else {
                    // 执行错误，直接标记所有命令执行失败
                    commands.forEach(command -> {
                        if (command.getIsError()) {
                            chatMessages.add(ChatMessageDTO.builder()
                                    .toolCallId(command.getCallId())
                                    .name(command.getFunction())
                                    .role(MessageRoleEnum.TOOL.getCode())
                                    .content(JSON.toJSONString(Collections.singletonList(command.getRspData())))
                                    .build());
                        } else {
                            chatMessages.add(ChatMessageDTO.builder()
                                    .role(MessageRoleEnum.TOOL.getCode())
                                    .name(command.getFunction())
                                    .toolCallId(command.getCallId())
                                    .content(JSON.toJSONString(HomeControlRspDTO.builder()
                                            .errorMsg(Objects.isNull(vehResultDTO.getMsg()) ? "设备服务器异常" : vehResultDTO.getMsg())
                                            .resultCode(vehResultDTO.getCode())
                                            .build()))
                                    .build());
                        }
                    });
                }
                isControl.set(true);
            }
            // STEP 回复用户
            AgentChatHomeControlResultDTO stepHomeControlResultDTO = AgentChatHomeControlResultDTO.ofDefault(message.getConversationId(), messageId)
                    .content("答案生成").event(ChatMessageEventEnum.STEP.getCode());
            sink.next(stepHomeControlResultDTO);
            OpenAICompletionRequestDTO chatCompletionRequestDTO = OpenAICompletionRequestDTO.builder().messages(chatMessages).stream(true).build();
            Flux<AgentChatHomeControlResultDTO> responseDTOFlux = chatOpenAIClient.stream(chatCompletionRequestDTO)
                    .flatMap(response -> {
                        if (Objects.isNull(response.getChoices())) {
                            log.warn("openai接口返回结果为空:{}", JSON.toJSONString(response));
                            return Mono.empty();
                        } else {
                            return Flux.just(response.getChoices().stream().map(choice -> {
                                if (Objects.nonNull(choice.getDelta().getToolCalls())) {
                                    log.info("openai返回工具调用：{}", JSON.toJSONString(choice));
                                    homeControlResultDTO.setContent("抱歉，我还没有学会这项技能，请您换个说法试一试。");
                                    homeControlResultDTO.setIsFinal(true);
                                } else {
                                    log.debug("openai返回对话内容：{}", JSON.toJSONString(choice));
                                    homeControlResultDTO.setContent(homeControlResultDTO.getContent() + choice.getDelta().getContent());
                                    homeControlResultDTO.setContentFormat("markdown");
                                    homeControlResultDTO.setLlmType("reply");
                                    homeControlResultDTO.setAnswerType(0);
                                    homeControlResultDTO.setIsEvil(false);
                                    homeControlResultDTO.setIsControl(isControl.get());
                                    if (ObjectUtils.isEmpty(choice.getFinishReason()) || "tool_calls".equals(choice.getFinishReason())) {
                                        homeControlResultDTO.setIsFinal(false);
                                    } else {
                                        homeControlResultDTO.setIsFinal(true);
                                    }
                                }
                                return homeControlResultDTO;
                            }).toArray(AgentChatHomeControlResultDTO[]::new));
                        }
                    })
                    .doOnComplete(sink::complete)
                    .doOnError(sink::error);
            responseDTOFlux.subscribe(sink::next);
        });
        controlResultDTOFlux.takeUntil(AgentChatHomeControlResultDTO::getIsFinal);
        return this.buildChatReturnFlux(message, recognition, preChat, controlResultDTOFlux, homeControlResultDTO).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Msg<? extends AgentPostChatResultDTO> postChat(AgentChatMessageDTO message, AgentIntentionRecognitionResultDTO intentionRecognition, AgentPreChatResultDTO preChat, AgentChatResultDTO chatResult) throws Exception {
        // 保存，对话记录
        AgentPostChatResultDTO result = new AgentPostChatHomeResultDTO();
        AgentChatConversationDO chatConversation = AgentChatConversationDO.of(message);
        AgentChatHomeControlResultDTO homeControlResultDTO = (AgentChatHomeControlResultDTO) chatResult;
        chatConversation.setId(homeControlResultDTO.getMessageId());
        chatConversation.setContent(homeControlResultDTO.getContent());
        chatConversation.setAnswerStatus(chatResult.getAnswerStatus());
        chatConversation.setAnswerType(chatResult.getAnswerType());
        chatConversation.setFrom(1);
        chatConversation.setType(3);
        mongoTemplate.insert(chatConversation, "agent_chat_conversation");
        return Msg.success(result).build();
    }
}
