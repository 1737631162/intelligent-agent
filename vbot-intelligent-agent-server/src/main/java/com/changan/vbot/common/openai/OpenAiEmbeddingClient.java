package com.changan.vbot.common.openai;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.changan.vbot.common.enums.AgentErrorCodeEnum;
import com.changan.vbot.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Slf4j
public class OpenAiEmbeddingClient {

    private OpenAiEmbeddingConfigProperties configProperties;

    public OpenAiEmbeddingClient(OpenAiEmbeddingConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    public List<Float> embed(String text) {
        OpenAIEmbeddingRequest embeddingRequest = OpenAIEmbeddingRequest.builder()
                .input(Collections.singletonList(text))
                .build();
        Mono<OpenAIEmbeddingResponseDTO> embeddingResponseDTOMono = this.call(embeddingRequest);
        OpenAIEmbeddingResponseDTO embeddingResponseDTO = embeddingResponseDTOMono.block();
        return embeddingResponseDTO.getData().get(0).getEmbedding();
    }

    private OpenAIEmbeddingRequest buildParam(OpenAIEmbeddingRequest request) {
        request.setModel(configProperties.getModel());
        request.setEncodingFormat(configProperties.getEncodingFormat());
        request.setDimensions(configProperties.getDimensions());
        return request;
    }

    public Mono<OpenAIEmbeddingResponseDTO> call(OpenAIEmbeddingRequest embeddingRequest) {
        embeddingRequest = this.buildParam(embeddingRequest);
        String url = String.format("%s%s", configProperties.getBaseUrl(), configProperties.getEmbeddingApi());
        HttpRequest httpRequest = HttpUtil.createPost(url);
        if (!ObjectUtils.isEmpty(configProperties.getApiKey())) {
            httpRequest.header(HttpHeaders.AUTHORIZATION, configProperties.getApiKey());
        }
        httpRequest.header(HttpHeaders.CONTENT_TYPE, "application/json");
        HttpResponse httpResponse = httpRequest.body(JSON.toJSONString(embeddingRequest)).execute();
        if (!httpResponse.isOk()) {
            log.warn("调用openai embedding接口失败，url:{}, response: {}", url, httpResponse.body());
            throw new BizException(AgentErrorCodeEnum.LLM_SERVER_ERROR);
        }
        OpenAIEmbeddingResponseDTO embeddingResponseDTO = JSON.parseObject(httpResponse.body(), OpenAIEmbeddingResponseDTO.class);
        return Mono.just(embeddingResponseDTO);
    }


}
