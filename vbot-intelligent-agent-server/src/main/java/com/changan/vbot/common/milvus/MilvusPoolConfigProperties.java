package com.changan.vbot.common.milvus;

import lombok.Data;

import java.time.Duration;

@Data
public class MilvusPoolConfigProperties {
    private int maxIdlePerKey = 5;
    private int minIdlePerKey = 0;
    private int maxTotalPerKey = 10;
    private int maxTotal = 50;
    private Duration maxWaitDuration = Duration.ofSeconds(3);
}
