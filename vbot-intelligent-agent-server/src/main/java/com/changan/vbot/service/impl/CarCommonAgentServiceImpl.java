package com.changan.vbot.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.changan.carbond.result.Msg;
import com.changan.vbot.common.enums.AgentChatAnswerTypeEnum;
import com.changan.vbot.common.enums.AgentErrorCodeEnum;
import com.changan.vbot.common.enums.AgentIdEnum;
import com.changan.vbot.common.enums.ChatTypeEnum;
import com.changan.vbot.common.exception.BizException;
import com.changan.vbot.dal.mongo.entity.AgentChatConversationDO;
import com.changan.vbot.service.BeanFactory;
import com.changan.vbot.service.IAgentConfigService;
import com.changan.vbot.service.IAgentService;
import com.changan.vbot.service.dto.AgentChatConditionResultDTO;
import com.changan.vbot.service.dto.AgentChatControlResultDTO;
import com.changan.vbot.service.dto.AgentChatMessageDTO;
import com.changan.vbot.service.dto.AgentChatQuestionResultDTO;
import com.changan.vbot.service.dto.AgentChatResultDTO;
import com.changan.vbot.service.dto.AgentIntentionRecognitionConditionResultDTO;
import com.changan.vbot.service.dto.AgentIntentionRecognitionControlResultDTO;
import com.changan.vbot.service.dto.AgentIntentionRecognitionQuestionResultDTO;
import com.changan.vbot.service.dto.AgentIntentionRecognitionResultDTO;
import com.changan.vbot.service.dto.AgentPostChatQuestionResultDTO;
import com.changan.vbot.service.dto.AgentPostChatResultDTO;
import com.changan.vbot.service.dto.AgentPreChatConditionResultDTO;
import com.changan.vbot.service.dto.AgentPreChatControlResultDTO;
import com.changan.vbot.service.dto.AgentPreChatQuestionResultDTO;
import com.changan.vbot.service.dto.AgentPreChatResultDTO;
import com.changan.vbot.service.dto.ChatTypeDTO;
import com.changan.vbot.service.dto.ChatTypeRespDTO;
import com.changan.vbot.service.manage.feign.TextClassificationClient;
import com.changan.vbot.service.thirdpart.TencentLkeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 通用智能体（安小宝）
 */
@Service("car_common_agent")
@Slf4j
public class CarCommonAgentServiceImpl implements IAgentService {

    @Resource
    private IAgentConfigService configService;

    @Resource
    private BeanFactory beanFactory;

    @Resource
    private TencentLkeService tencentLkeService;

    @Resource
    private TextClassificationClient textClassificationClient;

    @Value("${pre-cls-model.default-confidence:0.95}")
    private Double defaultConfidence;

    @Value("${pre-cls-model.query-confidence:0.88}")
    private Double queryConfidence;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public String getAgentId() {
        return AgentIdEnum.COMMON.getAgentId();
    }

    @Override
    public String getAgentName() {
        return AgentIdEnum.COMMON.getAgentName();
    }

    @Override
    public Msg<? extends AgentIntentionRecognitionResultDTO> intentionRecognition(AgentChatMessageDTO message) {
        Msg<AgentIntentionRecognitionResultDTO> result = Msg.<AgentIntentionRecognitionResultDTO>error(AgentErrorCodeEnum.SUCCESS).build();
        // 完善业务逻辑
        // 当triggerAgentId不等于agentId时，走对应的agentId做意图识别。
        // 当triggerAgentId等于agentId时，调用预分类模型进行意图识别，再根据预分类结果，控车、车况查询、通用问答走不通意图识别。
//        if (message.getTriggerAgentId().equals(message.getAgentId())) {
        //改为agentId为car_common_agent就走预分类
        if (message.getAgentId().equals("car_common_agent")) {
            ChatTypeDTO chatTypeDTO = new ChatTypeDTO();
            chatTypeDTO.setTexts(Collections.singletonList(message.getContent()));
            long start = System.currentTimeMillis();
            Msg<List<ChatTypeRespDTO>> rspData = textClassificationClient.getTextClassification(chatTypeDTO);
            log.info("调用预分类模型耗时：{}秒", (System.currentTimeMillis() - start) / 1000.0);
            if (!rspData.isSuccess()) {
                result.setMsg(rspData.getMsg());
                result.setCode(rspData.getCode());
                return result;
            }
            // 置信度判断，默认走问答
            int label = rspData.getData().get(0).getLabel();
            double confidence = rspData.getData().get(0).getConfidence();
            if ((label == 0 && confidence < queryConfidence) || (label != 0 && confidence < defaultConfidence)) {
                label = 1;
            }
            switch (label) {
                case 0:
                    message.setAgentId(AgentIdEnum.CONDITION.getAgentId());
                    return beanFactory.getAgentService(message.getAgentId()).intentionRecognition(message);
                case 1:
                    AgentIntentionRecognitionQuestionResultDTO questionResult = new AgentIntentionRecognitionQuestionResultDTO();
                    questionResult.setLabel(label);
                    result.setData(questionResult);
                    break;
                case 2:
                    message.setAgentId(AgentIdEnum.CONTROL.getAgentId());
                    return beanFactory.getAgentService(message.getAgentId()).intentionRecognition(message);
                default:
                    result.setMsg("未知意图");
                    result.setCode(AgentErrorCodeEnum.NO_MATCH.getCode());
            }
            return result;
        } else {
            IAgentService agentService = beanFactory.getAgentService(message.getAgentId());
            if (Objects.isNull(agentService)) {
                result.setCode(AgentErrorCodeEnum.SOURCE_NOT_FOUND.getCode());
                result.setMsg("被调用智能体不存在。");
            }
            return agentService.intentionRecognition(message);
        }
    }

    @Override
    public Msg<? extends AgentPreChatResultDTO> preChat(AgentChatMessageDTO message, AgentIntentionRecognitionResultDTO intentionRecognition) throws Exception {
        Msg<AgentPreChatResultDTO> result = Msg.<AgentPreChatResultDTO>error(AgentErrorCodeEnum.SUCCESS).build();
        // 完善业务逻辑
        if (intentionRecognition instanceof AgentIntentionRecognitionQuestionResultDTO) {
            AgentPreChatQuestionResultDTO questionResult = new AgentPreChatQuestionResultDTO();
            // 如果要对通用智能体的问答范围做限制或拉取配置信息，可以在这里增加处理逻辑，如，不同渠道进入的通用智能体，拉取配置信息
            AgentChatConversationDO chatConversation = AgentChatConversationDO.of(message);
            BeanUtil.copyProperties(message, chatConversation);
            if (Objects.isNull(message.getMessageId())) {
                message.setMessageId(UUID.randomUUID().toString());
            }
            if (StrUtil.isEmpty(message.getConversationId())) {
                String conversationId = UUID.randomUUID().toString();
                message.setConversationId(conversationId);
                chatConversation.setConversationId(conversationId);
            }
            chatConversation.setId(message.getMessageId());
            chatConversation.setFrom(0);
            chatConversation.setType(1);
            mongoTemplate.insert(chatConversation, "agent_chat_conversation");
            result.setData(questionResult);
            return result;
        } else if (intentionRecognition instanceof AgentIntentionRecognitionConditionResultDTO) {
            message.setAgentId(AgentIdEnum.CONDITION.getAgentId());
            return beanFactory.getAgentService(AgentIdEnum.CONDITION.getAgentId()).preChat(message, intentionRecognition);
        } else if (intentionRecognition instanceof AgentIntentionRecognitionControlResultDTO) {
            message.setAgentId(AgentIdEnum.CONTROL.getAgentId());
            return beanFactory.getAgentService(AgentIdEnum.CONTROL.getAgentId()).preChat(message, intentionRecognition);
        }
        return result;
    }

    @Override
    public Msg<? extends AgentChatResultDTO> chat(AgentChatMessageDTO message, AgentIntentionRecognitionResultDTO intentionRecognition, AgentPreChatResultDTO preChat) throws Exception {
        Msg<AgentChatResultDTO> result = Msg.<AgentChatResultDTO>error(AgentErrorCodeEnum.SUCCESS).build();
        // 完善业务逻辑
        if (preChat instanceof AgentPreChatQuestionResultDTO) {
            Msg<AgentChatQuestionResultDTO> chatMsg = tencentLkeService.chat(message.getUserId(), message.getConversationId(), message.getContent(), 0);
            result.setCode(chatMsg.getCode());
            result.setMsg(chatMsg.getMsg());
            result.setData(chatMsg.getData());
        } else if (preChat instanceof AgentPreChatConditionResultDTO) {
            return beanFactory.getAgentService(AgentIdEnum.CONDITION.getAgentId()).chat(message, intentionRecognition, preChat);
        } else if (preChat instanceof AgentPreChatControlResultDTO) {
            return beanFactory.getAgentService(AgentIdEnum.CONTROL.getAgentId()).chat(message, intentionRecognition, preChat);
        } else {
            result.setCode(AgentErrorCodeEnum.NO_MATCH.getCode());
            result.setMsg(AgentErrorCodeEnum.NO_MATCH.getMsg());
        }
        return result;
    }

    @Override
    public Flux<? extends AgentChatResultDTO> streamChat(AgentChatMessageDTO message, AgentIntentionRecognitionResultDTO intentionRecognition, AgentPreChatResultDTO preChat) throws Exception {
        AgentChatQuestionResultDTO chatResult = new AgentChatQuestionResultDTO();
        chatResult.setType(ChatTypeEnum.QA.getCode());
        if (preChat instanceof AgentPreChatConditionResultDTO) {
            return beanFactory.getAgentService(AgentIdEnum.CONDITION.getAgentId()).streamChat(message, intentionRecognition, preChat);
        } else if (preChat instanceof AgentPreChatControlResultDTO) {
            return beanFactory.getAgentService(AgentIdEnum.CONTROL.getAgentId()).streamChat(message, intentionRecognition, preChat);
        } else if (preChat instanceof AgentPreChatQuestionResultDTO) {
            Flux<? extends AgentChatResultDTO> lkeFlux = tencentLkeService.streamChat(message.getUserId(), message.getConversationId(), message.getContent(), chatResult, 0);
            return this.buildChatReturnFlux(message, intentionRecognition, preChat, lkeFlux, chatResult);
        } else {
            throw new BizException(AgentErrorCodeEnum.NO_MATCH);
        }
    }

    @Override
    public Msg<? extends AgentPostChatResultDTO> postChat(AgentChatMessageDTO message, AgentIntentionRecognitionResultDTO intentionRecognition, AgentPreChatResultDTO preChat, AgentChatResultDTO chatResult) throws Exception {
        AgentPostChatResultDTO agentPostChatQuestionResult = new AgentPostChatQuestionResultDTO();
        Msg<AgentPostChatResultDTO> result = Msg.success(agentPostChatQuestionResult).build();
        // 完善业务逻辑
        if (chatResult instanceof AgentChatConditionResultDTO) {
            return beanFactory.getAgentService(AgentIdEnum.CONDITION.getAgentId()).postChat(message, intentionRecognition, preChat, chatResult);
        } else if (chatResult instanceof AgentChatControlResultDTO) {
            return beanFactory.getAgentService(AgentIdEnum.CONTROL.getAgentId()).postChat(message, intentionRecognition, preChat, chatResult);
        } else if (chatResult instanceof AgentChatQuestionResultDTO) {
            AgentChatConversationDO chatConversation = AgentChatConversationDO.of(message);
            BeanUtil.copyProperties(message, chatConversation);
            AgentChatQuestionResultDTO questionResult = (AgentChatQuestionResultDTO) chatResult;
            chatConversation.setId(questionResult.getMessageId());
            chatConversation.setRefId(message.getMessageId());
            chatConversation.setContent(questionResult.getContent());
            chatConversation.setAnswerType(AgentChatAnswerTypeEnum.TYPE_LLM_REPLY.getType());
            chatConversation.setFrom(1);
            chatConversation.setType(questionResult.getType());
            chatConversation.setIsEvil(questionResult.getIsEvil());
            chatConversation.setIsFinal(questionResult.getIsFinal());
            chatConversation.setAnswerStatus(questionResult.getAnswerStatus());
            chatConversation.setRecommendeds(questionResult.getRecommendeds());
            mongoTemplate.insert(chatConversation, "agent_chat_conversation");
        } else {
            log.warn("postChat failed to match: message:{},preChat:{},chatResult:{}", JSON.toJSONString(message), JSON.toJSONString(preChat), JSON.toJSONString(chatResult));
            result.setCode(AgentErrorCodeEnum.NO_MATCH.getCode());
            result.setMsg(AgentErrorCodeEnum.NO_MATCH.getMsg());
        }
        return result;
    }


}
