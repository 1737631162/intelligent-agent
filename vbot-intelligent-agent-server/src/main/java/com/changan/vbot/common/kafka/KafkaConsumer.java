package com.changan.vbot.common.kafka;

import com.alibaba.fastjson2.JSON;
import com.changan.vbot.service.IHomeDeviceService;
import com.changan.vbot.service.dto.DeviceMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

@Component
@Slf4j
public class KafkaConsumer {

    @Resource
    private IHomeDeviceService deviceService;

    @KafkaListener(topics = "${spring.kafka.consumer.topic.device-sync}")
    public void dealDeviceMessage(String value) {
        log.info("device info:{}", value);
        DeviceMessageDTO messageDTO = JSON.parseObject(value, DeviceMessageDTO.class);
        if (Objects.nonNull(messageDTO.getType())) {
            if (0 == messageDTO.getType()) {
                deviceService.syncDeviceInfo(messageDTO);
            } else if (1 == messageDTO.getType()) {
                deviceService.syncSceneInfo(messageDTO);
            } else if(2== messageDTO.getType()){
                deviceService.syncBrandInfo(messageDTO);
            } else {
                log.warn("type value is invalid,message:{}", value);
            }
        } else {
            log.warn("无效数据，缺少场景标识：{}", value);
        }
    }

}
