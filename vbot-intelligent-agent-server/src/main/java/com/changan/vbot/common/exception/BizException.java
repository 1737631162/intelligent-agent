package com.changan.vbot.common.exception;

import org.apache.commons.lang3.StringUtils;

import com.changan.carbond.ErrorCode.SYSTEM;
import com.changan.carbond.IErrorCode;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class BizException extends RuntimeException {
    
    /**
     * @Fields serialVersionUID
     * TODO（用一句话描述这个变量表示什么）
     */
    private static final long serialVersionUID = 4828524397090813712L;

    private int code;
    
    private String msg;
    
    public BizException() {
        super();
        this.code = SYSTEM.SYSTEM_EXCEPTION.getCode();
        this.msg = SYSTEM.SYSTEM_EXCEPTION.getMessage();
    }
    
    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public BizException(IErrorCode responseCode) {
        super();
        this.code = responseCode.getCode();
        this.msg = responseCode.getMessage();
    }
    
    public BizException(IErrorCode responseCode,String format) {
        super();
        this.code = responseCode.getCode();
        if(StringUtils.isBlank(format)) {
            format = "";
        }
        this.msg = String.format(responseCode.getMessage(), format);
    }
}
