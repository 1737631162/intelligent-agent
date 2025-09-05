package com.changan.vbot.web.controller;

import com.changan.carbond.result.Msg;
import com.changan.vbot.common.vo.PageResultVO;
import com.changan.vbot.service.IAgentManagementService;
import com.changan.vbot.service.IAgentResourceService;
import com.changan.vbot.service.dto.AgentManagementDTO;
import com.changan.vbot.service.dto.AgentResourceDTO;
import com.changan.vbot.web.request.AgentRecommendedSkillConfigRequestVO;
import com.changan.vbot.web.request.AgentSeriesConfigRequestVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Api(tags = "智能体资源接口")
@RestController
@AllArgsConstructor
@RequestMapping("/admin/inner-api/v1/agent-resource")
@Validated
public class AgentResourceController {

    @Autowired
    private IAgentResourceService agentResourceService;

    @PostMapping("/add")
    @ApiOperation("新增资源接口")
    public Msg<String> add(@RequestBody AgentResourceDTO resource) {
        return agentResourceService.add(resource);
    }

    @PostMapping("/list")
    @ApiOperation("资源查询接口")
    public Msg<PageResultVO<AgentResourceDTO>> list(@RequestBody AgentResourceDTO request) {
        return agentResourceService.list(request);
    }

    @GetMapping("/get-detail")
    @ApiOperation("资源详情查询接口")
    public Msg<AgentResourceDTO> getDetail(@RequestParam String id) {
        return agentResourceService.getDetail(id);
    }

    @PostMapping("/edit")
    @ApiOperation("编辑智能体接口")
    public Msg<String> edit(@RequestBody AgentResourceDTO resource) {
        return agentResourceService.edit(resource);
    }

    @GetMapping("/change-status")
    @ApiOperation("状态更改接口")
    public Msg<String> changeStatus(@RequestParam String id, Integer status) {
        return agentResourceService.changeStatus(id ,status);
    }

    @GetMapping("/delete")
    @ApiOperation("资源删除接口")
    public Msg<String> delete(@RequestParam String id) {
        return agentResourceService.delete(id);
    }

}
