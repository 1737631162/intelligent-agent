package com.changan.vbot.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VheResultDTO<T> {
    private String code;
    private String msg;
    private T data;
    private Boolean success;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date responseTime;

    public static <T> VheResultDTO<T> error(String code, String msg) {
        return VheResultDTO.<T>builder().code(code).msg(msg).success(false).responseTime(new Date()).build();
    }
}
