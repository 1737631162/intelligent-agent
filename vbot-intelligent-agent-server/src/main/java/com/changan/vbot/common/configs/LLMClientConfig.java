package com.changan.vbot.common.configs;

import com.changan.vbot.common.openai.OpenAIClient;
import com.changan.vbot.common.openai.OpenAIConfigProperties;
import com.changan.vbot.common.openai.OpenAiEmbeddingClient;
import com.changan.vbot.common.openai.OpenAiEmbeddingConfigProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LLMClientConfig {

    @Bean("rewriteOpenAIConfigProperties")
    @ConfigurationProperties(prefix = "openai.rewrite")
    public OpenAIConfigProperties rewriteOpenAIConfigProperties() {
        return new OpenAIConfigProperties();
    }

    @Bean("rewriteOpenAIClient")
    public OpenAIClient rewriteOpenAIClient(@Qualifier("rewriteOpenAIConfigProperties") OpenAIConfigProperties openAIConfigProperties) {
        return new OpenAIClient(openAIConfigProperties);
    }

    @Bean("extractOpenAIConfigProperties")
    @ConfigurationProperties(prefix = "openai.extract")
    public OpenAIConfigProperties extractOpenAIConfigProperties() {
        return new OpenAIConfigProperties();
    }

    @Bean("extractOpenAIClient")
    public OpenAIClient extractOpenAIClient(@Qualifier("extractOpenAIConfigProperties") OpenAIConfigProperties openAIConfigProperties) {
        return new OpenAIClient(openAIConfigProperties);
    }

    @Bean("chatOpenAIConfigProperties")
    @ConfigurationProperties(prefix = "openai.chat")
    public OpenAIConfigProperties chatOpenAIConfigProperties() {
        return new OpenAIConfigProperties();
    }

    @Bean("chatOpenAIClient")
    public OpenAIClient chatOpenAIClient(@Qualifier("chatOpenAIConfigProperties") OpenAIConfigProperties openAIConfigProperties) {
        return new OpenAIClient(openAIConfigProperties);
    }


    @Bean
    @ConfigurationProperties(prefix = "openai.embedding")
    public OpenAiEmbeddingConfigProperties openAiEmbeddingConfigProperties() {
        return new OpenAiEmbeddingConfigProperties();
    }

    @Bean
    public OpenAiEmbeddingClient openAiEmbeddingClient(OpenAiEmbeddingConfigProperties openAiEmbeddingConfigProperties) {
        return new OpenAiEmbeddingClient(openAiEmbeddingConfigProperties);
    }


}
