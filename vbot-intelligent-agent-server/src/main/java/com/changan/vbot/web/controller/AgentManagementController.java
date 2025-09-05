package com.changan.vbot.web.controller;

import com.changan.carbond.result.Msg;
import com.changan.vbot.service.IAgentManagementService;
import com.changan.vbot.service.dto.AgentManagementDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Api(tags = "智能体管理C端接口")
@RestController
@AllArgsConstructor
@RequestMapping("/inner-api/v1/agent-management")
@Validated
public class AgentManagementController {

    @Autowired
    private IAgentManagementService agentManagementService;

    @GetMapping("/list")
    @ApiOperation("智能体查询接口")
    public Msg<List<AgentManagementDTO>> list(@RequestParam String carSeries,
                                              @RequestParam String userId, @RequestParam String carId) {
        return agentManagementService.userList(carSeries, userId, carId);
    }

    @GetMapping("/add-agent")
    @ApiOperation("用户新增智能体接口")
    public Msg<String> userAddAgent(@RequestParam String userId, @RequestParam String agentId,
                                    @RequestParam String carId) {
        return agentManagementService.userAddAgent(userId, agentId, carId);
    }

    @GetMapping("/delete-agent")
    @ApiOperation("用户删除智能体接口")
    public Msg<String> userDeleteAgent(@RequestParam String userId, @RequestParam String agentId,
                                       @RequestParam String carId) {
        return agentManagementService.userDeleteAgent(userId, agentId, carId);
    }
}
