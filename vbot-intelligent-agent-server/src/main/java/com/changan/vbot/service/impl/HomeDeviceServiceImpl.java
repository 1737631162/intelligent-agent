package com.changan.vbot.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.changan.vbot.common.constants.ChatConstants;
import com.changan.vbot.common.constants.HomeDeviceConstants;
import com.changan.vbot.common.enums.AgentErrorCodeEnum;
import com.changan.vbot.common.enums.HomeContentTypeEnum;
import com.changan.vbot.common.enums.HomeControlActionEnum;
import com.changan.vbot.common.enums.HomeControlFunctionEnum;
import com.changan.vbot.common.enums.HomeControlResultCodeEnum;
import com.changan.vbot.common.enums.HomeTypeEnum;
import com.changan.vbot.common.exception.BizException;
import com.changan.vbot.common.openai.OpenAIEmbedding;
import com.changan.vbot.common.openai.OpenAIEmbeddingRequest;
import com.changan.vbot.common.openai.OpenAIEmbeddingResponseDTO;
import com.changan.vbot.common.openai.OpenAIToolCallDTO;
import com.changan.vbot.common.openai.OpenAIToolCallDataDTO;
import com.changan.vbot.common.openai.OpenAiEmbeddingClient;
import com.changan.vbot.dal.entity.HomeDocument;
import com.changan.vbot.service.IHomeDeviceService;
import com.changan.vbot.service.dto.DeviceMessageDTO;
import com.changan.vbot.service.dto.DeviceMessageDataDTO;
import com.changan.vbot.service.dto.HomeControlReqDTO;
import com.changan.vbot.service.dto.HomeControlRspDTO;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.milvus.pool.MilvusClientV2Pool;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.DeleteResp;
import io.milvus.v2.service.vector.response.QueryResp;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HomeDeviceServiceImpl implements IHomeDeviceService {

    private static final String MILVUS_COLLECTION = "home_document";

    @Resource
    private MilvusClientV2Pool milvusClientV2Pool;

    @Resource
    private OpenAiEmbeddingClient openAiEmbeddingClient;

    @Value("${home.min.similarity:0.7}")
    private Float homeMinSimilarity;

    @Value("${home.min.command-similarity:0.5}")
    private Float commandSimilarity;

    private static ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public void syncDeviceInfo(DeviceMessageDTO messageDTO) {

        if (Objects.nonNull(messageDTO.getEvent())) {
            switch (messageDTO.getEvent()) {
                case 0:
                    this.insertDevice(messageDTO.getData(), messageDTO.getUserId());
                    break;
                case 1:
                    this.deleteDevice(messageDTO.getData().getDeviceId(), messageDTO.getUserId());
                    this.insertDevice(messageDTO.getData(), messageDTO.getUserId());
                    break;
                case 2:
                    this.deleteDevice(messageDTO.getData().getDeviceId(), messageDTO.getUserId());
                    break;
                default:
                    log.warn("event value is invalid,message:{}", messageDTO);
                    break;
            }
        } else {
            log.warn("无效设备数据，缺少事件标识：{}", messageDTO);
        }
    }


    @Override
    public void syncSceneInfo(DeviceMessageDTO messageDTO) {
        if (Objects.nonNull(messageDTO.getEvent())) {
            switch (messageDTO.getEvent()) {
                case 0:
                    this.insertScene(messageDTO.getData(), messageDTO.getUserId());
                    break;
                case 1:
                    this.deleteScene(messageDTO.getData().getSceneId(), messageDTO.getUserId());
                    this.insertScene(messageDTO.getData(), messageDTO.getUserId());
                    break;
                case 2:
                    this.deleteScene(messageDTO.getData().getSceneId(), messageDTO.getUserId());
                    break;
                default:
                    log.warn("event value is invalid,message:{}", messageDTO);
                    break;
            }
        } else {
            log.warn("无效场景数据，缺少事件标识：{}", messageDTO);
        }
    }

    @Override
    public List<OpenAIToolCallDataDTO<HomeControlReqDTO, HomeControlRspDTO>> convertToCommand(String userId, String command, List<OpenAIToolCallDTO> toolCalls) {
        return toolCalls.parallelStream().map(toolCall -> this.convertToCommand(userId, command, toolCall)).collect(Collectors.toList());
    }

    @Override
    public void syncBrandInfo(DeviceMessageDTO messageDTO) {
        if (Objects.nonNull(messageDTO.getEvent())) {
            if (messageDTO.getEvent() == 2) {
                String key = UUID.randomUUID().toString().replace("-", "");
                MilvusClientV2 milvusClientV2 = this.getClient(key);
                try {
                    DeleteReq deleteReq = DeleteReq.builder().collectionName(MILVUS_COLLECTION)
                            .filter(String.format("metadata[\"userId\"]==\"%s\" and metadata[\"type\"]==%d and metadata[\"brandCode\"]==\"%s\"", messageDTO.getUserId(), HomeTypeEnum.DEVICE.getCode(), messageDTO.getData().getBrandCode()))
                            .build();
                    DeleteResp deleteResp = milvusClientV2.delete(deleteReq);
                    log.info("用户：{},解绑品牌:{}，删除了{}个文档", messageDTO.getUserId(), messageDTO.getData().getBrandCode(), deleteResp.getDeleteCnt());
                } finally {
                    this.milvusClientV2Pool.returnClient(key, milvusClientV2);
                }
            }
        } else {
            log.warn("invalid data ,the event is null:{}", JSON.toJSONString(messageDTO));
        }
    }

    @SneakyThrows
    private OpenAIToolCallDataDTO<HomeControlReqDTO, HomeControlRspDTO> convertToCommand(String userId, String command, OpenAIToolCallDTO toolCall) {
        OpenAIToolCallDataDTO<HomeControlReqDTO, HomeControlRspDTO> toolCallDataDTO = OpenAIToolCallDataDTO.<HomeControlReqDTO, HomeControlRspDTO>builder()
                .callId(toolCall.getId()).function(toolCall.getFunction().getName()).isError(true).build();
        if (Objects.isNull(toolCall.getFunction().getJsonArguments())) {
            toolCallDataDTO.setRspData(HomeControlRspDTO.builder().commandId(UUID.randomUUID().toString()).errorMsg("无效arguments参数，json格式化失败").resultCode(HomeControlResultCodeEnum.FAIL.getCode()).build());
            return toolCallDataDTO;
        }
        JSONObject arguments = toolCall.getFunction().getJsonArguments();
        HomeControlActionEnum action = HomeControlActionEnum.of(arguments.getString(HomeDeviceConstants.ACTION));
        if (action.equals(HomeControlActionEnum.UNKNOWN)) {
            toolCallDataDTO.setRspData(HomeControlRspDTO.builder().commandId(UUID.randomUUID().toString()).errorMsg("无效action参数").resultCode(HomeControlResultCodeEnum.FAIL.getCode()).build());
            return toolCallDataDTO;
        }
        HomeControlFunctionEnum function = HomeControlFunctionEnum.of(toolCall.getFunction().getName());
        // 如果是场景，走场景名称匹配
        // 如果是未知设备，走设备名称匹配
        List<HomeDocument> homeDocuments;
        String clientKey = UUID.randomUUID().toString().replace("-", "");
        boolean isAll = arguments.getBooleanValue(HomeDeviceConstants.IS_ALL, false);
        switch (function) {
            case UNKNOWN_FUNCTION:
                toolCallDataDTO.setRspData(HomeControlRspDTO.builder().commandId(UUID.randomUUID().toString()).errorMsg(String.format("不支持该函数：%s", toolCall.getFunction().getName()))
                        .resultCode(HomeControlResultCodeEnum.FAIL.getCode()).build());
                return toolCallDataDTO;
            case SCENE_MODE_CONTROL:
                String living = arguments.getString(HomeDeviceConstants.LIVING);
                if (isAll) {
                    QueryReq queryReq = QueryReq.builder()
                            .collectionName(MILVUS_COLLECTION)
                            .filter(String.format("metadata[\"userId\"]==\"%s\" and metadata[\"type\"]==%d", userId, HomeTypeEnum.SCENE.getCode()))
                            .build();
                    homeDocuments = this.queryDocument(queryReq, clientKey);
                } else {
                    if (ObjectUtils.isEmpty(living)) {
                        toolCallDataDTO.setRspData(HomeControlRspDTO.builder().commandId(UUID.randomUUID().toString()).errorMsg("living参数为空，无法进行场景控制")
                                .resultCode(HomeControlResultCodeEnum.FAIL.getCode()).build());
                        return toolCallDataDTO;
                    }
                    homeDocuments = this.searchDocument(living, HomeTypeEnum.SCENE.getCode(), HomeContentTypeEnum.NAME, null, userId, clientKey);
                    if (homeDocuments.isEmpty()) {
                        toolCallDataDTO.setRspData(HomeControlRspDTO.builder().commandId(UUID.randomUUID().toString()).errorMsg("找不到该场景")
                                .resultCode(HomeControlResultCodeEnum.FAIL.getCode()).build());
                        return toolCallDataDTO;
                    } else if (homeDocuments.size() > 1) {
                        String msg = String.format("控制失败：找到%d个场景，无法确定控制那个场景，场景名称列表为：", homeDocuments.size(), homeDocuments.stream().map(HomeDocument::getContent).collect(Collectors.joining("、")));
                        toolCallDataDTO.setRspData(HomeControlRspDTO.builder().commandId(UUID.randomUUID().toString()).errorMsg(msg)
                                .data(homeDocuments.stream().map(HomeDocument::getMetadata).collect(Collectors.toList()))
                                .resultCode(HomeControlResultCodeEnum.FAIL.getCode()).build());
                        return toolCallDataDTO;
                    }
                }
                toolCallDataDTO.setIsError(false);
                toolCallDataDTO.setReqData(homeDocuments.stream().map(homeDocument -> {
                    JsonObject sceneMetadata = homeDocument.getMetadata();
                    return HomeControlReqDTO.builder().commandId(UUID.randomUUID().toString())
                            .action(action.getAction()).type(HomeTypeEnum.SCENE.getCode())
                            .id(sceneMetadata.get("sceneId").getAsString())
                            .userId(userId).build();
                }).collect(Collectors.toList()));
                return toolCallDataDTO;
            case UNKNOWN_DEVICE_CONTROL:
                String targetName = arguments.getString(HomeDeviceConstants.TARGET_NAME);
                if (ObjectUtils.isEmpty(targetName)) {
                    toolCallDataDTO.setRspData(HomeControlRspDTO.builder().commandId(UUID.randomUUID().toString()).errorMsg("targetName参数为空，无法进行设备控制")
                            .resultCode(HomeControlResultCodeEnum.FAIL.getCode()).build());
                    return toolCallDataDTO;
                }
                homeDocuments = this.searchDocument(targetName, HomeTypeEnum.DEVICE.getCode(), HomeContentTypeEnum.NAME, null, userId, clientKey);
                if (homeDocuments.isEmpty()) {
                    toolCallDataDTO.setRspData(HomeControlRspDTO.builder().commandId(UUID.randomUUID().toString()).errorMsg("找不到该设备")
                            .resultCode(HomeControlResultCodeEnum.FAIL.getCode()).build());
                    return toolCallDataDTO;
                } else if (homeDocuments.size() > 1) {
                    String deviceNames = homeDocuments.stream().map(document -> {
                        if (document.getMetadata().has("deviceName")) {
                            return document.getMetadata().get("deviceName").getAsString();
                        } else {
                            return document.getContent();
                        }
                    }).collect(Collectors.joining("、"));
                    String msg = String.format("控制失败：找到%d个设备，无法确定控制那个设备，设备名称列表：", homeDocuments.size(), deviceNames);
                    toolCallDataDTO.setRspData(HomeControlRspDTO.builder().commandId(UUID.randomUUID().toString()).errorMsg(msg)
                            .data(homeDocuments.stream().map(HomeDocument::getMetadata).collect(Collectors.toList()))
                            .resultCode(HomeControlResultCodeEnum.FAIL.getCode()).build());
                    return toolCallDataDTO;
                } else {
                    JsonObject deviceMetadata = homeDocuments.get(0).getMetadata();
                    toolCallDataDTO.setIsError(false);
                    toolCallDataDTO.setReqData(Collections.singletonList(HomeControlReqDTO.builder().commandId(UUID.randomUUID().toString()).action(action.getAction())
                            .type(HomeTypeEnum.DEVICE.getCode()).id(deviceMetadata.get("deviceId").getAsString())
                            .userId(userId).build()));
                    return toolCallDataDTO;
                }
            default:
                // 异步提交获取设备列表
                String loc = arguments.getString(HomeDeviceConstants.LOC);
                String brand = arguments.getString(HomeDeviceConstants.BRAND);
                JSONObject params = arguments.clone();
                params.remove(HomeDeviceConstants.INDEX);
                params.remove(HomeDeviceConstants.BRAND);
                params.remove(HomeDeviceConstants.LOC);
                params.remove(HomeDeviceConstants.ACTION);
                params.remove(HomeDeviceConstants.TARGET_NAME);
                params.remove(HomeDeviceConstants.IS_ALL);
                // 根据指令匹配设备名称
                Future<List<HomeDocument>> commandHomeDocumentsFuture = executorService.submit(() -> this.searchDocument(String.format(ChatConstants.INSTRUCT_TEMPLATE_DEVICE_INFO, command), HomeTypeEnum.DEVICE.getCode(), HomeContentTypeEnum.NAME, function.getCategoryCode(), userId, clientKey + "_0", commandSimilarity));
                if (ObjectUtils.isEmpty(loc) && ObjectUtils.isEmpty(brand)) {
                    QueryReq queryReq = QueryReq.builder()
                            .filter(String.format("metadata[\"type\"]==%d and metadata[\"contentType\"]==%d and metadata[\"categoryCode\"]==\"%s\" and metadata[\"userId\"]==\"%s\""
                                    , HomeTypeEnum.DEVICE.getCode(), HomeContentTypeEnum.CATEGORY.getCode(), function.getCategoryCode(), userId))
                            .collectionName(MILVUS_COLLECTION)
                            .limit(10)
                            .build();
                    homeDocuments = this.queryDocument(queryReq, clientKey);
                } else if (ObjectUtils.isEmpty(loc) && !ObjectUtils.isEmpty(brand)) {
                    // 品牌不为空时
                    homeDocuments = this.searchDocument(brand, HomeTypeEnum.DEVICE.getCode(), HomeContentTypeEnum.BRAND, function.getCategoryCode(), userId, clientKey);
                } else if (!ObjectUtils.isEmpty(loc) && ObjectUtils.isEmpty(brand)) {
                    // 位置不为空时
                    homeDocuments = this.searchDocument(loc, HomeTypeEnum.DEVICE.getCode(), HomeContentTypeEnum.TAG, function.getCategoryCode(), userId, clientKey);
                } else {
                    // 都不为空时
                    Future<List<HomeDocument>> locHomeDocumentsFuture = executorService.submit(() -> this.searchDocument(loc, HomeTypeEnum.DEVICE.getCode(), HomeContentTypeEnum.CATEGORY, function.getCategoryCode(), userId, clientKey + "_1"));
                    Future<List<HomeDocument>> brandHomeDocumentsFuture = executorService.submit(() -> this.searchDocument(brand, HomeTypeEnum.DEVICE.getCode(), HomeContentTypeEnum.CATEGORY, function.getCategoryCode(), userId, clientKey + "_2"));
                    // 取两个结果并取交集
                    List<HomeDocument> locHomeDocuments = locHomeDocumentsFuture.get();
                    List<HomeDocument> brandHomeDocuments = brandHomeDocumentsFuture.get();
                    List<String> deviceIds = locHomeDocuments.stream().map(homeDocument -> homeDocument.getMetadata().get("deviceId").getAsString()).collect(Collectors.toList());
                    homeDocuments = brandHomeDocuments.stream().filter(homeDocument -> deviceIds.contains(homeDocument.getMetadata().get("deviceId").getAsString())).collect(Collectors.toList());
                }
                String index = arguments.getString(HomeDeviceConstants.INDEX);
                List<HomeDocument> nameHomeDocuments = commandHomeDocumentsFuture.get();
                if (!nameHomeDocuments.isEmpty()) {
                    if (homeDocuments.isEmpty()) {
                        toolCallDataDTO.setIsError(false);
                        HomeDocument homeDocument = nameHomeDocuments.get(0);
                        toolCallDataDTO.setReqData(Collections.singletonList(this.buildHomeDeviceIdControlReq(homeDocument, action, params, userId)));
                        return toolCallDataDTO;
                    } else {
                        List<String> deviceIds = homeDocuments.stream().map(homeDocument -> homeDocument.getMetadata().get("deviceId").getAsString()).collect(Collectors.toList());
                        homeDocuments = nameHomeDocuments.stream().filter(homeDocument -> deviceIds.contains(homeDocument.getMetadata().get("deviceId").getAsString())).collect(Collectors.toList());
                        if (isAll && !homeDocuments.isEmpty()) {
                            toolCallDataDTO.setIsError(false);
                            toolCallDataDTO.setReqData(homeDocuments.stream().map(homeDocument -> this.buildHomeDeviceIdControlReq(homeDocument, action, params, userId)).collect(Collectors.toList()));
                            return toolCallDataDTO;
                        }
                        if (homeDocuments.isEmpty()) {
                            toolCallDataDTO.setRspData(HomeControlRspDTO.builder().commandId(UUID.randomUUID().toString()).errorMsg("找不到该设备")
                                    .resultCode(HomeControlResultCodeEnum.FAIL.getCode()).build());
                            return toolCallDataDTO;
                        }
                        if (homeDocuments.size() > 1) {
                            String deviceNames = this.getDeviceNames(homeDocuments);
                            String msg = String.format("控制失败：找到%d个%s设备，无法确定控制那个设备，设备名称列表为：%s", homeDocuments.size(), function.getDescription(), deviceNames);
                            toolCallDataDTO.setRspData(HomeControlRspDTO.builder().commandId(UUID.randomUUID().toString()).errorMsg(msg)
                                    .data(homeDocuments.stream().map(HomeDocument::getMetadata).collect(Collectors.toList()))
                                    .resultCode(HomeControlResultCodeEnum.FAIL.getCode()).build());
                            return toolCallDataDTO;
                        }
                        toolCallDataDTO.setIsError(false);
                        toolCallDataDTO.setReqData(Collections.singletonList(this.buildHomeDeviceIdControlReq(nameHomeDocuments.get(0), action, params, userId)));
                        return toolCallDataDTO;
                    }
                } else {
                    if (homeDocuments.isEmpty()) {
                        toolCallDataDTO.setRspData(HomeControlRspDTO.builder().commandId(UUID.randomUUID().toString()).errorMsg("找不到该设备")
                                .resultCode(HomeControlResultCodeEnum.FAIL.getCode()).build());
                        return toolCallDataDTO;
                    } else if (!isAll && (homeDocuments.size() > 1 && Objects.isNull(index))) {
                        String deviceNames = this.getDeviceNames(homeDocuments);
                        String msg = String.format("控制失败：找到%d个%s设备，无法确定控制那个设备，设备名称列表为：%s", homeDocuments.size(), function.getDescription(), deviceNames);
                        toolCallDataDTO.setRspData(HomeControlRspDTO.builder().commandId(UUID.randomUUID().toString()).errorMsg(msg)
                                .data(homeDocuments.stream().map(HomeDocument::getMetadata).collect(Collectors.toList()))
                                .resultCode(HomeControlResultCodeEnum.FAIL.getCode()).build());
                        return toolCallDataDTO;
                    } else if (!isAll && (homeDocuments.size() > 1 && homeDocuments.size() < Integer.parseInt(index))) {
                        toolCallDataDTO.setRspData(HomeControlRspDTO.builder().commandId(toolCall.getId())
                                .errorMsg(String.format("控制失败：找到%d个%s设备，index参数超出范围", homeDocuments.size(), function.getDescription()))
                                .data(homeDocuments.stream().map(HomeDocument::getMetadata).collect(Collectors.toList()))
                                .resultCode(HomeControlResultCodeEnum.FAIL.getCode()).build());
                        return toolCallDataDTO;
                    } else if (!isAll && (homeDocuments.size() > 1 && homeDocuments.size() >= Integer.parseInt(index))) {
                        // 非唯一设备，但是index参数有效
                        toolCallDataDTO.setIsError(false);
                        homeDocuments.sort(Comparator.comparing(o -> {
                            if (o.getMetadata().has("deviceName")) {
                                return o.getMetadata().get("deviceName").getAsString();
                            } else {
                                return o.getMetadata().get("deviceId").getAsString();
                            }
                        }));
                        toolCallDataDTO.setReqData(Collections.singletonList(this.buildHomeDeviceIdControlReq(homeDocuments.get(Integer.parseInt(index) - 1), action, params, userId)));
                        return toolCallDataDTO;
                    } else {
                        // 唯一设备或批量控制情况
                        toolCallDataDTO.setIsError(false);
                        toolCallDataDTO.setReqData(homeDocuments.stream().map(homeDocument -> this.buildHomeDeviceIdControlReq(homeDocument, action, params, userId))
                                .collect(Collectors.toList()));
                        return toolCallDataDTO;
                    }
                }
        }
    }

    private String getDeviceNames(List<HomeDocument> homeDocuments) {
        return homeDocuments.stream().map(document -> {
            if (document.getMetadata().has("deviceName")) {
                return document.getMetadata().get("deviceName").getAsString();
            } else {
                return document.getContent();
            }
        }).collect(Collectors.joining("、"));
    }

    private HomeControlReqDTO buildHomeDeviceIdControlReq(HomeDocument homeDocument, HomeControlActionEnum action, Map<String, Object> params, String userId) {
        return HomeControlReqDTO.builder()
                .commandId(UUID.randomUUID().toString())
                .action(action.getAction())
                .type(HomeTypeEnum.DEVICE.getCode())
                .id(homeDocument.getMetadata().get("deviceId").getAsString())
                .params(params)
                .userId(userId).build();
    }

    private List<HomeDocument> queryDocument(QueryReq queryReq, String clientKey) {
        MilvusClientV2 milvusClientV2 = this.getClient(clientKey);
        try {
            queryReq.setOutputFields(Arrays.asList("id", "content", "metadata"));
            QueryResp queryResp = milvusClientV2.query(queryReq);
            return queryResp.getQueryResults().stream().map(queryResult -> {
                HomeDocument homeDocument = new HomeDocument();
                homeDocument.setId((Long) queryResult.getEntity().get("id"));
                homeDocument.setContent((String) queryResult.getEntity().get("content"));
                homeDocument.setMetadata((JsonObject) queryResult.getEntity().get("metadata"));
                return homeDocument;
            }).collect(Collectors.toList());
        } finally {
            milvusClientV2Pool.returnClient(clientKey, milvusClientV2);
        }
    }

    private List<HomeDocument> searchDocument(String text, Integer dataType, HomeContentTypeEnum contentType, String categoryCode, String userId, String clientKey) {
        return searchDocument(text, dataType, contentType, categoryCode, userId, clientKey, homeMinSimilarity);
    }

    private List<HomeDocument> searchDocument(String text, Integer dataType, HomeContentTypeEnum contentType, String categoryCode, String userId, String clientKey, float minSimilarity) {
        List<Float> embedding = openAiEmbeddingClient.embed(text);
        MilvusClientV2 milvusClientV2 = this.getClient(clientKey);
        try {
            FloatVec floatVec = new FloatVec(embedding);
            String filter = String.format("metadata[\"type\"]==%d and metadata[\"contentType\"]==%d and metadata[\"userId\"]==\"%s\"", dataType, contentType.getCode(), userId);
            if (Objects.nonNull(categoryCode)) {
                filter += String.format(" and metadata[\"categoryCode\"]==\"%s\"", categoryCode);
            }
            SearchReq searchReq = SearchReq.builder().collectionName(MILVUS_COLLECTION)
                    .data(Collections.singletonList(floatVec))
                    .filter(filter)
                    .outputFields(Arrays.asList("id", "content", "metadata"))
                    .topK(20)
                    .build();
            SearchResp searchResp = milvusClientV2.search(searchReq);
            return searchResp.getSearchResults().get(0).stream().filter(searchResult -> searchResult.getScore() >= minSimilarity).map(searchResult -> {
                HomeDocument homeDocument = new HomeDocument();
                homeDocument.setId((Long) searchResult.getId());
                homeDocument.setContent((String) searchResult.getEntity().get("content"));
                homeDocument.setMetadata((JsonObject) searchResult.getEntity().get("metadata"));
                homeDocument.setScore(searchResult.getScore());
                return homeDocument;
            }).collect(Collectors.toList());
        } finally {
            milvusClientV2Pool.returnClient(clientKey, milvusClientV2);
        }
    }

    private MilvusClientV2 getClient(String clientKey) {
        MilvusClientV2 milvusClientV2 = milvusClientV2Pool.getClient(clientKey);
        if (Objects.isNull(milvusClientV2)) {
            milvusClientV2 = milvusClientV2Pool.getClient(clientKey);
            if (Objects.isNull(milvusClientV2)) {
                throw new BizException(AgentErrorCodeEnum.MILVUS_CLIENT_ERROR);
            }
        }
        return milvusClientV2;
    }

    private void deleteScene(String sceneId, String userId) {
        String key = UUID.randomUUID().toString().replace("-", "");
        MilvusClientV2 milvusClientV2 = this.getClient(key);
        try {
            DeleteReq deleteReq = DeleteReq.builder().collectionName(MILVUS_COLLECTION)
                    .filter(String.format("metadata['sceneId']==\"%s\" and metadata['userId']==\"%s\"", sceneId, userId)).build();
            DeleteResp deleteResp = milvusClientV2.delete(deleteReq);
            log.info("删除场景ID：{}，删除文档数：{}", sceneId, deleteResp.getDeleteCnt());
        } finally {
            milvusClientV2Pool.returnClient(key, milvusClientV2);
        }
    }

    private void insertScene(DeviceMessageDataDTO messageDataDTO, String userId) {
        String key = UUID.randomUUID().toString().replace("-", "");
        MilvusClientV2 milvusClientV2 = this.getClient(key);
        try {
            List<Float> embedding = openAiEmbeddingClient.embed(messageDataDTO.getSceneName());
            List<JsonObject> data = new LinkedList<>();
            // 构造场景名称
            JsonObject nameMetadata = new JsonObject();
            nameMetadata.addProperty("sceneId", messageDataDTO.getSceneId());
            nameMetadata.addProperty("type", 1);
            nameMetadata.addProperty("contentType", HomeContentTypeEnum.NAME.getCode());
            nameMetadata.addProperty("userId", userId);
            nameMetadata.addProperty("sceneName", messageDataDTO.getSceneName());
            JsonObject nameJsonObject = new JsonObject();
            nameJsonObject.addProperty("content", messageDataDTO.getSceneName());
            nameJsonObject.add("embedding", this.embeddingToJsonArray(embedding));
            nameJsonObject.add("metadata", nameMetadata);
            data.add(nameJsonObject);
            InsertReq insertReq = InsertReq.builder().collectionName(MILVUS_COLLECTION)
                    .data(data)
                    .build();
            milvusClientV2.insert(insertReq);
            log.info("insert scene info success,sceneId:{}", messageDataDTO.getSceneId());
        } finally {
            milvusClientV2Pool.returnClient(key, milvusClientV2);
        }
    }

    private List<List<Float>> generateEmbedding(List<String> contents) {
        OpenAIEmbeddingRequest embeddingRequest = OpenAIEmbeddingRequest.builder().input(contents).build();
        Mono<OpenAIEmbeddingResponseDTO> embeddingResponseDTOMono = this.openAiEmbeddingClient.call(embeddingRequest);
        OpenAIEmbeddingResponseDTO embeddingResponseDTO = embeddingResponseDTOMono.block();
        return embeddingResponseDTO.getData().stream().map(OpenAIEmbedding::getEmbedding).collect(Collectors.toList());
    }

    private void insertDevice(DeviceMessageDataDTO messageDataDTO, String userId) {
        String key = UUID.randomUUID().toString().replace("-", "");
        MilvusClientV2 milvusClientV2 = this.getClient(key);
        try {
            List<String> contents = new LinkedList<>();
            contents.add(messageDataDTO.getDeviceName());
            contents.add(messageDataDTO.getDeviceCategory());
            contents.add(messageDataDTO.getBrandName());
            if (!ObjectUtils.isEmpty(messageDataDTO.getTags())) {
                contents.addAll(messageDataDTO.getTags());
            }
            List<List<Float>> embeddings = this.generateEmbedding(contents);
            List<JsonObject> data = new LinkedList<>();
            // 构造设备名称
            JsonObject nameMetadata = this.buildDeviceMetadata(messageDataDTO, HomeTypeEnum.DEVICE.getCode(), userId, HomeContentTypeEnum.NAME.getCode());
            JsonObject nameJsonObject = new JsonObject();
            nameJsonObject.addProperty("content", messageDataDTO.getDeviceName());
            nameJsonObject.add("embedding", this.embeddingToJsonArray(embeddings.get(0)));
            nameJsonObject.add("metadata", nameMetadata);
            data.add(nameJsonObject);
            //构造设备品类
            JsonObject categoryMetadata = this.buildDeviceMetadata(messageDataDTO, HomeTypeEnum.DEVICE.getCode(), userId, HomeContentTypeEnum.CATEGORY.getCode());
            JsonObject categoryJsonObject = new JsonObject();
            categoryJsonObject.addProperty("content", messageDataDTO.getDeviceCategory());
            categoryJsonObject.add("embedding", this.embeddingToJsonArray(embeddings.get(1)));
            categoryJsonObject.add("metadata", categoryMetadata);
            data.add(categoryJsonObject);
            //构造设备品牌
            JsonObject brandMetadata = this.buildDeviceMetadata(messageDataDTO, HomeTypeEnum.DEVICE.getCode(), userId, HomeContentTypeEnum.BRAND.getCode());
            JsonObject brandJsonObject = new JsonObject();
            brandJsonObject.addProperty("content", messageDataDTO.getBrandName());
            brandJsonObject.add("embedding", this.embeddingToJsonArray(embeddings.get(2)));
            brandJsonObject.add("metadata", brandMetadata);
            data.add(brandJsonObject);
            // 构造设备标签
            if (!ObjectUtils.isEmpty(messageDataDTO.getTags())) {
                for (int i = 0; i < messageDataDTO.getTags().size(); i++) {
                    JsonObject tagMetadata = this.buildDeviceMetadata(messageDataDTO, HomeTypeEnum.DEVICE.getCode(), userId, HomeContentTypeEnum.TAG.getCode());
                    JsonObject tagJsonObject = new JsonObject();
                    tagJsonObject.addProperty("content", messageDataDTO.getTags().get(i));
                    tagJsonObject.add("embedding", this.embeddingToJsonArray(embeddings.get(i + 3)));
                    tagJsonObject.add("metadata", tagMetadata);
                    data.add(tagJsonObject);
                }
            }
            InsertReq insertReq = InsertReq.builder().collectionName(MILVUS_COLLECTION)
                    .data(data)
                    .build();
            milvusClientV2.insert(insertReq);
            log.info("insert device info success,deviceId:{}", messageDataDTO.getDeviceId());
        } finally {
            milvusClientV2Pool.returnClient(key, milvusClientV2);
        }
    }

    private JsonObject buildDeviceMetadata(DeviceMessageDataDTO messageDataDTO, int type, String userId, int contentType) {
        JsonObject metadata = new JsonObject();
        metadata.addProperty("deviceId", messageDataDTO.getDeviceId());
        metadata.addProperty("deviceName", messageDataDTO.getDeviceName());
        metadata.addProperty("type", type);
        metadata.addProperty("userId", userId);
        metadata.addProperty("contentType", contentType);
        metadata.addProperty("categoryCode", messageDataDTO.getCategoryCode());
        metadata.addProperty("brandCode", messageDataDTO.getBrandCode());
        return metadata;
    }

    private JsonArray embeddingToJsonArray(List<Float> embedding) {
        JsonArray jsonArray = new JsonArray();
        for (Float f : embedding) {
            jsonArray.add(f);
        }
        return jsonArray;
    }

    private void deleteDevice(String deviceId, String userId) {
        String key = UUID.randomUUID().toString().replace("-", "");
        MilvusClientV2 milvusClientV2 = this.getClient(key);
        try {
            DeleteReq deleteReq = DeleteReq.builder().collectionName(MILVUS_COLLECTION)
                    .filter(String.format("metadata['deviceId']==\"%s\" and metadata['userId']==\"%s\"", deviceId, userId)).build();
            DeleteResp deleteResp = milvusClientV2.delete(deleteReq);
            log.info("删除设备ID：{}，删除文档数：{}", deviceId, deleteResp.getDeleteCnt());
        } finally {
            milvusClientV2Pool.returnClient(key, milvusClientV2);
        }
    }
}
