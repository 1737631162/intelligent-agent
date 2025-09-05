package com.changan.vbot.web.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.changan.carbond.result.Msg;
import com.changan.vbot.common.enums.AgentErrorCodeEnum;
import com.changan.vbot.service.ITripCardPreferenceService;
import com.changan.vbot.service.dto.TripCardPreferenceDTO;
import com.changan.vbot.service.dto.VehicleTripCardDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import java.lang.reflect.Type;
import java.util.List;


@Api(tags = {"智能备车接口"})
@Validated
@RestController
@RequestMapping("/inner-api/smart-trip")
@Slf4j
public class IntelligentCarPreparationController {

    @Value("${smart-trip.domain}")
    private String domain;

    @Autowired
    ITripCardPreferenceService tripCardPreferenceService;

    private static final Type SCRIPTSTRING_RESPONSE_TYPE = new TypeReference<Msg<String>>() {
    }.getType();

    private static final Type SCRIPTLIST_RESPONSE_TYPE = new TypeReference<Msg<List<VehicleTripCardDTO>>>() {
    }.getType();

    private static final Type SCRIPTSTRING_RESPONSE_INTEGER = new TypeReference<Msg<Integer>>() {
    }.getType();

    private static final Type TRIP_CARD_RESPONSE_TYPE = new TypeReference<Msg<VehicleTripCardDTO>>() {
    }.getType();

    /**
     * 新增备车卡片
     */
    @ApiOperation("新增备车卡片")
    @PostMapping("/create")
    public Msg<VehicleTripCardDTO> createPlanCar(@RequestBody VehicleTripCardDTO tripCardReq) {
        if (StrUtil.isNotEmpty(tripCardReq.getCarId()) && StrUtil.isNotEmpty(tripCardReq.getUserId())) {
            String url = domain + "/inner-api/smart-trip/vehicle/v1.0/create" + "?userId=" + tripCardReq.getUserId();
            try {
                log.info("createPlanCar: url{} params{}", url, JSON.toJSONString(tripCardReq));
                String response = HttpUtil.post(url, JSON.toJSONString(tripCardReq));
                return JSON.parseObject(response, TRIP_CARD_RESPONSE_TYPE);
            } catch (Exception e) {
                log.error("createPlanCar error, url{} ", url, e);
                return Msg.<VehicleTripCardDTO>error(AgentErrorCodeEnum.SYSTEM_EXCEPTION).build();
            }
        }
        return Msg.<VehicleTripCardDTO>error(AgentErrorCodeEnum.MISSING_PARAM_IS).build();
    }

    /**
     * 查询备车列表
     */
    @ApiOperation("查询备车列表")
    @GetMapping("/list")
    public Msg<List<VehicleTripCardDTO>> selectCarListById(@NotBlank(message = "车辆id不能为空") @RequestParam String carId,
                                                           @NotBlank(message = "用户id不能为空") @RequestParam String userId) {
        String url = domain + "/inner-api/smart-trip/vehicle/v1.0/list?carId="
                + carId
                + "&userId="
                + userId;
        try {
            String response = HttpUtil.get(url);
            log.info("selectCarListById, url{}, resp:{}", url, response);
            return JSON.parseObject(response, SCRIPTLIST_RESPONSE_TYPE);
        } catch (Exception e) {
            log.error("selectCarListById error {}", e.getMessage());
            return Msg.<List<VehicleTripCardDTO>>error(AgentErrorCodeEnum.SYSTEM_EXCEPTION).build();
        }
    }

    /**
     * 更新备车卡片
     *
     * @return
     */
    @ApiOperation("更新备车卡片")
    @PostMapping("update/{id}")
    public Msg<String> updatePlanCarById(@RequestBody VehicleTripCardDTO tripCardReq,
                                         @NotBlank(message = "备车卡片ID不能为空") @PathVariable String id) {
        if (StrUtil.isNotEmpty(tripCardReq.getCarId()) && StrUtil.isNotEmpty(tripCardReq.getUserId())) {
            String url = domain + "/inner-api/smart-trip/vehicle/v1.0/update/" + id + "?userId=" + tripCardReq.getUserId();
            try {
                String response = HttpUtil.post(url, JSON.toJSONString(tripCardReq));
                log.info("updatePlanCarById  url{},resp{}", url, response);
                return JSON.parseObject(response, SCRIPTSTRING_RESPONSE_TYPE);
            } catch (Exception e) {
                log.error("updatePlanCarById error {} ", e.getMessage());
                return Msg.<String>error(AgentErrorCodeEnum.SYSTEM_EXCEPTION).build();
            }
        }
        return Msg.<String>error(AgentErrorCodeEnum.MISSING_PARAM_IS).build();
    }

    /**
     * 删除备车卡片
     *
     * @return
     */
    @ApiOperation("删除备车卡片")
    @PostMapping("delete/{id}")
    public Msg<String> deletePlanCar(@NotBlank(message = "备车卡片ID不能为空") @PathVariable String id,
                                     @NotBlank(message = "车辆id不能为空") @RequestParam String carId,
                                     @NotBlank(message = "用户id不能为空") @RequestParam String userId) {

        String url = domain + "/inner-api/smart-trip/vehicle/v1.0/delete/" + id + "?userId=" + userId;
        JSONObject payload = new JSONObject();
        payload.put("carId", carId);
        payload.put("userId", userId);
        try {
            String response = HttpUtil.post(url, payload.toJSONString());
            log.info("deletePlanCar url: {} resp: {}", url, payload.toJSONString());
            return JSON.parseObject(response, SCRIPTSTRING_RESPONSE_TYPE);
        } catch (Exception e) {
            log.error("deletePlanCar error {} ", e.getMessage());
            return Msg.<String>error(AgentErrorCodeEnum.SYSTEM_EXCEPTION).build();
        }
    }

    /**
     * 立即备车
     *
     * @return
     */
    @ApiOperation("立即备车")
    @PostMapping("execute/{id}")
    public Msg<String> nowPlanCar(@NotBlank(message = "备车卡片ID不能为空") @PathVariable String id,
                                  @NotBlank(message = "车辆id不能为空") @RequestParam String carId,
                                  @NotBlank(message = "用户id不能为空") @RequestParam String userId) {
        String url = domain + "/inner-api/smart-trip/v1.0/execute/" + id + "?userId=" + userId;
        JSONObject payload = new JSONObject();
        payload.put("carId", carId);
        payload.put("userId", userId);
        try {
            String response = HttpUtil.post(url, payload.toJSONString());
            log.info("nowPlanCar  url = {} , resp = {}", url, response);
            return JSON.parseObject(response, SCRIPTSTRING_RESPONSE_TYPE);
        } catch (Exception e) {
            log.error("nowPlanCar error , url{} ", url, e);
            return Msg.<String>error(AgentErrorCodeEnum.SYSTEM_EXCEPTION).build();
        }
    }

    /**
     * 备车卡片执行状态查询
     */
    @ApiOperation("备车卡片执行状态查询")
    @GetMapping("execute/status")
    public Msg<Integer> selectCarStatusById(@NotBlank(message = "历史任务Id不能为空") @RequestParam String taskId,
                                            @NotBlank(message = "用户id不能为空") @RequestParam String userId) {
        String url = domain + "/inner-api/smart-trip/v1.0/execute/status?taskId="
                + taskId + "&userId="
                + userId;
        try {
            String response = HttpUtil.get(url);
            log.info("selectCarStatusById, url{}, resp:{}", url, response);
            return JSON.parseObject(response, SCRIPTSTRING_RESPONSE_INTEGER);
        } catch (Exception e) {
            log.error("selectCarStatusById error, url{}", url, e);
            return Msg.<Integer>error(AgentErrorCodeEnum.SYSTEM_EXCEPTION).build();
        }
    }

    /**
     * 设置备车偏好选项
     */
    @ApiOperation("设置备车偏好选项")
    @PostMapping("preference/set")
    public Msg<String> setPreference(@RequestBody TripCardPreferenceDTO tripCardPreference) {
        return tripCardPreferenceService.setPreference(tripCardPreference);
    }

    /**
     * 获取备车偏好选项
     */
    @ApiOperation("获取备车偏好选项")
    @GetMapping("preference/get")
    public Msg<TripCardPreferenceDTO> getPreference(@RequestParam String userId, @RequestParam String carId) {
        return tripCardPreferenceService.getPreference(userId, carId);
    }

}
