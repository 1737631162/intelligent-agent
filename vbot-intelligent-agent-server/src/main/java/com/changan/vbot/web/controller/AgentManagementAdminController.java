package com.changan.vbot.web.controller;

import com.changan.carbond.result.Msg;
import com.changan.vbot.service.IAgentManagementService;
import com.changan.vbot.service.dto.AgentManagementDTO;
import com.changan.vbot.service.dto.AgentRelatedModelDTO;
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
@Api(tags = "智能体管理接口")
@RestController
@AllArgsConstructor
@RequestMapping("/admin/inner-api/v1/agent-management")
@Validated
public class AgentManagementAdminController {

    @Autowired
    private IAgentManagementService agentManagementService;

    @PostMapping("/list")
    @ApiOperation("智能体查询接口")
    public Msg<List<AgentManagementDTO>> list(@RequestBody AgentManagementDTO request) {
        return agentManagementService.list(request);
    }

    @GetMapping("/get-detail")
    @ApiOperation("智能体详情查询接口")
    public Msg<AgentManagementDTO> getDetail(@RequestParam String agentId) {
        return agentManagementService.getDetail(agentId);
    }

    @PostMapping("/series-config")
    @ApiOperation("智能体车系配置接口")
    public Msg<String> seriesConfig(@RequestBody AgentSeriesConfigRequestVO request) {
        return agentManagementService.seriesConfig(request.getAgentId(), request.getCarSeries());
    }

    @PostMapping("/recommended-skill-config")
    @ApiOperation("智能体推荐技能配置接口")
    public Msg<String> recommendedSkillConfig(@RequestBody AgentRecommendedSkillConfigRequestVO request) {
        return agentManagementService.recommendedSkillConfig(request.getAgentId(), request.getSkills());
    }

    @PostMapping("/add-agent")
    @ApiOperation("新增智能体接口")
    public Msg<String> addAgent(@RequestBody AgentManagementDTO agent) {
        return agentManagementService.addAgent(agent);
    }

    @PostMapping("/edit")
    @ApiOperation("编辑智能体接口")
    public Msg<String> edit(@RequestBody AgentManagementDTO agent) {
        return agentManagementService.edit(agent);
    }

    @GetMapping("/model-list")
    @ApiOperation("智能体关联模型查询接口")
    public Msg<List<AgentRelatedModelDTO>> modelList(@RequestParam Integer source) {
        return agentManagementService.modelList(source);
    }

    @PostMapping("/add-model")
    @ApiOperation("新增智能体接口")
    public Msg<String> addModel(@RequestBody AgentRelatedModelDTO model) {
        return agentManagementService.addModel(model);
    }

}
