package com.changan.vbot.service.manage.feign;

import com.changan.vbot.service.dto.HomeControlQueryReqDTO;
import com.changan.vbot.service.dto.HomeControlReqDTO;
import com.changan.vbot.service.dto.HomeControlRspDTO;
import com.changan.vbot.service.dto.VheResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DeviceControlClientHystrix implements DeviceControlClient {
    @Override
    public VheResultDTO<List<String>> control(List<HomeControlReqDTO> data) {
        return VheResultDTO.error("0500", "设备控制服务调用失败");
    }

    @Override
    public VheResultDTO<List<HomeControlRspDTO>> queryControlResult(List<HomeControlQueryReqDTO> data) {
        return VheResultDTO.error("0500", "设备控制结果查询服务调用失败");
    }
}
