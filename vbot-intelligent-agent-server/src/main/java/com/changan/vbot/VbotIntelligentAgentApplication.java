package com.changan.vbot;

import com.changan.common.core.annotation.CaSpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@CaSpringBootApplication
//@EnableDubbo
@ServletComponentScan
@EnableFeignClients(basePackages = {"com.changan"})
public class VbotIntelligentAgentApplication {


    public static void main(String[] args) {
        SpringApplication.run(VbotIntelligentAgentApplication.class, args);
    }


}
