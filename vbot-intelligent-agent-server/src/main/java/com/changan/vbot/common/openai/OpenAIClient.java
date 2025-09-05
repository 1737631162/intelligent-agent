package com.changan.vbot.common.openai;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.changan.vbot.common.enums.AgentErrorCodeEnum;
import com.changan.vbot.common.enums.MessageRoleEnum;
import com.changan.vbot.common.exception.BizException;
import com.changan.vbot.common.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class OpenAIClient {

    private List<Object> tools;

    private OpenAIConfigProperties configProperties;

    public OpenAIClient(OpenAIConfigProperties configProperties) {
        this.configProperties = configProperties;
        if (!ObjectUtils.isEmpty(configProperties.getToolPath())) {
            this.tools = JSON.parseArray(FileUtils.readJarFile(configProperties.getToolPath()));
        }
    }

    private OpenAICompletionRequestDTO buildParam(OpenAICompletionRequestDTO completionRequest) {
        if (ObjectUtils.isEmpty(completionRequest.getMessages())) {
            throw new IllegalArgumentException("messages不能为空");
        }
        if (!ObjectUtils.isEmpty(this.tools)) {
            completionRequest.setTools(this.tools);
        }
        // 如果配置了系统提示词，且传入没有提示词，则加入默认配置的系统提示词
        if (Objects.nonNull(configProperties.getSystemPrompt()) && !MessageRoleEnum.SYSTEM.getCode().equalsIgnoreCase(completionRequest.getMessages().get(0).getRole())) {
            completionRequest.getMessages().add(0, ChatMessageDTO.builder().role(MessageRoleEnum.SYSTEM.getCode()).content(configProperties.getSystemPrompt()).build());
        }
        completionRequest.setModel(configProperties.getModel());
        completionRequest.setFrequencyPenalty(configProperties.getFrequencyPenalty());
        completionRequest.setMaxNewTokens(configProperties.getMaxNewTokens());
        completionRequest.setPresencePenalty(configProperties.getPresencePenalty());
        completionRequest.setTopK(configProperties.getTopK());
        completionRequest.setTopP(configProperties.getTopP());
        completionRequest.setTemperature(configProperties.getTemperature());
        return completionRequest;
    }

    public Mono<OpenAIResponseDTO> call(OpenAICompletionRequestDTO completionRequest) {
        completionRequest = this.buildParam(completionRequest);
        String url = String.format("%s%s", configProperties.getBaseUrl(), configProperties.getCompletionsApi());
        HttpRequest httpRequest = HttpUtil.createPost(url);
        if (!ObjectUtils.isEmpty(configProperties.getApiKey())) {
            httpRequest.header(HttpHeaders.AUTHORIZATION, configProperties.getApiKey());
        }
        httpRequest.header(HttpHeaders.CONTENT_TYPE, "application/json");
        String reqBody = JSON.toJSONString(completionRequest);
        log.info("大模型请求参数：{}", reqBody);
        HttpResponse httpResponse = httpRequest.body(reqBody).execute();
        if (!httpResponse.isOk()) {
            log.warn("调用openai接口失败，url:{}, response: {}", url, httpResponse.body());
            throw new BizException(AgentErrorCodeEnum.LLM_SERVER_ERROR);
        }
        OpenAIResponseDTO responseDTO = JSON.parseObject(httpResponse.body(), OpenAIResponseDTO.class);
        return Mono.just(responseDTO);
    }

    public Flux<OpenAIResponseDTO> stream(OpenAICompletionRequestDTO completionRequest) {
        completionRequest = this.buildParam(completionRequest);
        completionRequest.setStream(true);
        String url = String.format("%s%s", configProperties.getBaseUrl(), configProperties.getCompletionsApi());
        WebClient webClient = WebClient.builder().baseUrl(url).defaultHeader(HttpHeaders.AUTHORIZATION, configProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, "text/event-stream")
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().responseTimeout(Duration.ofSeconds(30))))
                .build();
        String reqBody = JSON.toJSONString(completionRequest);
        log.info("大模型请求参数：{}", reqBody);
        return webClient.post().bodyValue(reqBody).retrieve().bodyToFlux(String.class)
                .takeUntil(row -> row.equals("[DONE]"))
                .filter(row -> !row.equals("[DONE]"))
                .map(JSON::parseObject)
                .groupBy(jsonObject -> !ObjectUtils.isEmpty(jsonObject.getJSONArray("choices")) &&
                        Objects.nonNull(jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("delta").getJSONArray("tool_calls")))
                .flatMap(jsonGroup -> {
                    if (jsonGroup.key()) {
                        return jsonGroup.buffer().flatMap(buffer -> {
                            OpenAIResponseDTO toolResponseDTO = buffer.get(0).to(OpenAIResponseDTO.class);
                            List<JSONObject> toolCalls = buffer.stream().flatMap(toolRsp -> toolRsp.getJSONArray("choices").stream())
                                    .flatMap(choice -> ((JSONObject) choice).getJSONObject("delta").getJSONArray("tool_calls").stream())
                                    .map(toolCall -> (JSONObject) toolCall).collect(Collectors.toList());
                            OpenAIToolCallDTO toolCallDTO = toolCalls.get(0).to(OpenAIToolCallDTO.class);
                            String arguments = toolCalls.stream().filter(toolCall -> Objects.nonNull(toolCall.getJSONObject("function").getString("arguments")))
                                    .map(toolCall -> toolCall.getJSONObject("function").getString("arguments")).collect(Collectors.joining(""));
                            toolCallDTO.getFunction().setArguments(arguments);
                            toolResponseDTO.getChoices().get(0).getDelta().setToolCalls(Collections.singletonList(toolCallDTO));
                            return Flux.just(toolResponseDTO);
                        });
                    } else {
                        return jsonGroup.map(jsonObject -> jsonObject.to(OpenAIResponseDTO.class));
                    }
                });
    }
}
