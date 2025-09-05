package com.changan.vbot;

import com.changan.vbot.common.kafka.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest(classes = VbotIntelligentAgentApplication.class)
class VbotIntelligentAgentApplicationTests {

    @Resource
    private KafkaConsumer kafkaConsumer;

    @Test
    void testDealDeviceMessage() {
        String value = "{\"data\":{\"brandCode\":\"haier\",\"brandName\":\"海尔\",\"categoryCode\":\"AirPurifier\",\"deviceCategory\":\"空气净化器\",\"deviceId\":\"1958804552007979009\",\"deviceName\":\"空气净化器\",\"roomName\":\"全屋1\",\"tags\":[\"全屋1\"]},\"event\":\"1\",\"type\":\"0\",\"userId\":\"e0ba2ba35b9849bfbe9d8b17a59a31da\"}";
        kafkaConsumer.dealDeviceMessage(value);
    }

}
