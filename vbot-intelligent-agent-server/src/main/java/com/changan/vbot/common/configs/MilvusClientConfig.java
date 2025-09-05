package com.changan.vbot.common.configs;

import com.changan.vbot.common.milvus.MilvusConnectConfigProperties;
import com.changan.vbot.common.milvus.MilvusPoolConfigProperties;
import io.milvus.pool.MilvusClientV2Pool;
import io.milvus.pool.PoolConfig;
import io.milvus.v2.client.ConnectConfig;
import lombok.SneakyThrows;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusClientConfig {

    @Bean
    @ConfigurationProperties(prefix = "milvus")
    public MilvusConnectConfigProperties connectConfig() {
        return new MilvusConnectConfigProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "milvus.pool")
    public MilvusPoolConfigProperties poolConfig() {
        return new MilvusPoolConfigProperties();
    }

    @SneakyThrows
    @Bean
    public MilvusClientV2Pool milvusClientV2Pool(MilvusPoolConfigProperties poolConfig, MilvusConnectConfigProperties connectConfig) {
        return new MilvusClientV2Pool(PoolConfig.builder()
                .maxIdlePerKey(poolConfig.getMaxIdlePerKey())
                .minIdlePerKey(poolConfig.getMinIdlePerKey())
                .maxTotalPerKey(poolConfig.getMaxTotalPerKey())
                .maxTotal(poolConfig.getMaxTotal())
                .maxBlockWaitDuration(poolConfig.getMaxWaitDuration())
                .build(), ConnectConfig.builder()
                .uri(connectConfig.getUri())
                .token(connectConfig.getToken())
                .username(connectConfig().getUsername())
                .password(connectConfig().getPassword())
                .dbName(connectConfig.getDbName())
                .build());
    }
}
