package com.changan.vbot.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeControlRspDTO {

    private String commandId;
    private String errorMsg;
    private String resultCode; // 0 超时 1 成功 2 失败
    private String subErrorCode;

    @JsonIgnore
    private Object data;

    @JsonIgnore
    public boolean isFinished() {
        return "1".equals(resultCode) || "2".equals(resultCode);
    }
}
