package com.changan.vbot.common.configs;

import com.changan.carbond.metrics.registry.CarbondMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CarbondPrometheusConfig {
    /**
     * prometheus打点集成
     *
     * @return CarbondMeterRegistry
     */
    @Bean("carbondMeterRegistry")
    public CarbondMeterRegistry carbondMeterRegistry() {
        return new CarbondMeterRegistry();
    }

}
