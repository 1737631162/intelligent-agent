package com.changan.vbot.service;

import com.changan.carbond.result.Msg;
import com.changan.vbot.service.dto.AgentChatMessageDTO;
import com.changan.vbot.service.dto.AgentChatResultDTO;
import com.changan.vbot.service.dto.AgentIntentionRecognitionResultDTO;
import com.changan.vbot.service.dto.AgentPostChatResultDTO;
import com.changan.vbot.service.dto.AgentPreChatResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;

public interface IAgentService {


    /**
     * 获取agentId
     *
     * @return
     */
    String getAgentId();

    /**
     * 获取agentName
     *
     * @return
     */
    String getAgentName();


    /**
     * 意图识别
     *
     * @return 返回识别结果
     */
    Msg<? extends AgentIntentionRecognitionResultDTO> intentionRecognition(AgentChatMessageDTO message);

    /**
     * 前置业务逻辑处理（如权限校验）
     *
     * @param message
     * @return
     * @throws Exception
     */
    Msg<? extends AgentPreChatResultDTO> preChat(AgentChatMessageDTO message, AgentIntentionRecognitionResultDTO intentionRecognition) throws Exception;

    /**
     * 非流式对话（实际业务处理）
     *
     * @param message
     * @return
     * @throws Exception
     */
    Msg<? extends AgentChatResultDTO> chat(AgentChatMessageDTO message, AgentIntentionRecognitionResultDTO intentionRecognition, AgentPreChatResultDTO preChat) throws Exception;

    /**
     * 流式对话（实际业务处理）
     *
     * @param message
     * @return
     * @throws Exception
     */
    Flux<? extends AgentChatResultDTO> streamChat(AgentChatMessageDTO message, AgentIntentionRecognitionResultDTO intentionRecognition, AgentPreChatResultDTO preChat) throws Exception;

    default Flux<? extends AgentChatResultDTO> buildChatReturnFlux(AgentChatMessageDTO message, AgentIntentionRecognitionResultDTO intentionRecognition, AgentPreChatResultDTO preChat, Flux<? extends AgentChatResultDTO> flux, AgentChatResultDTO chatResult) {
        Logger log = LoggerFactory.getLogger(this.getClass());
        return flux.timeout(Duration.ofSeconds(30)).doOnComplete(() -> {
            log.info("streamChat onCompletion");
            try {
                chatResult.setIsFinal(true);
                chatResult.setAnswerStatus(1);
                postChat(message, intentionRecognition, preChat, chatResult);
            } catch (Exception e) {
                log.info("streamChat onCompletion error: {}", e.getMessage(), e);
            }
        }).doOnError((e) -> {
            log.info("streamChat onError error: {}", e.getMessage(), e);
            try {
                chatResult.setIsFinal(true);
                chatResult.setAnswerStatus(2);
                postChat(message, intentionRecognition, preChat, chatResult);
            } catch (Exception e2) {
                log.info("streamChat onError error: {}", e.getMessage(), e);
            }
        }).timeout(Duration.ofSeconds(30), (firstTimeout) -> {
            log.info("streamChat onTimeout");
            try {
                chatResult.setIsFinal(true);
                chatResult.setAnswerStatus(3);
                postChat(message, intentionRecognition, preChat, chatResult);
            } catch (Exception e) {
                log.info("streamChat onTimeout error: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 后置业务处理
     *
     * @param message
     * @return
     * @throws Exception
     */
    Msg<? extends AgentPostChatResultDTO> postChat(AgentChatMessageDTO message, AgentIntentionRecognitionResultDTO intentionRecognition, AgentPreChatResultDTO preChat, AgentChatResultDTO chatResult) throws Exception;
}
