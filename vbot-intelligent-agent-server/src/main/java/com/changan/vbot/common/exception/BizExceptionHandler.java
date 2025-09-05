package com.changan.vbot.common.exception;

import com.alibaba.fastjson2.JSON;
import com.changan.carbond.result.Msg;
import com.changan.common.core.exception.CustomException;
import com.changan.vbot.common.enums.AgentErrorCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

/**
 * @author 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class BizExceptionHandler {

    /**
     * 处理所有不可知的异常
     *
     * @param e 异常
     * @return Msg<Serializable>
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    Msg handleException(Exception e) {
        log.error(e.getMessage(), e);
        return Msg.error(AgentErrorCodeEnum.SYSTEM_EXCEPTION).build();
    }

    /**
     * 处理缺少参数异常
     *
     * @param e 异常
     * @return Msg<Serializable>
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    Msg handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("missing servlet request parameter：{}", e.getMessage());
        String errMsg = String.format(AgentErrorCodeEnum.MISSING_PARAM_IS.getMessage(), e.getParameterName());
        return new Msg(AgentErrorCodeEnum.MISSING_PARAM_IS.getCode(), errMsg, null);
    }

    /**
     * 处理所有业务异常
     *
     * @param e 异常
     * @return Msg<Serializable>
     */
    @ExceptionHandler(com.changan.carbond.exception.BizException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    Msg handleBusinessException(com.changan.carbond.exception.BizException e) {
        if (e.getData() != null && !e.getData().isEmpty()) {
            log.error("error msg:{},error params:{}", e.getMessage(), JSON.toJSONString(e.getData()), e);
        } else {
            log.error("error msg:{}", e.getMessage(), e);
        }
        return new Msg(e.getErrorCode().getCode(), e.getErrorMsg(), null);
    }

    /**
     * 运行异常 RuntimeException
     */
    @ExceptionHandler({BizException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public Msg handleBizException(BizException e) {
        log.error("error msg:{}", e.getMsg(), e);
        return new Msg(e.getCode(), e.getMsg(), null);
    }

    /**
     * 处理所有业务异常
     *
     * @param e 异常
     * @return Msg<Serializable>
     */
    @ExceptionHandler(CustomException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    Msg handleCustomException(CustomException e) {
        log.error("error msg:" + e.getMessage(), e);
        return Msg.error(e.getErrorCode()).build();
    }

    /**
     * 参数异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public Msg handleValidException(MethodArgumentNotValidException e) {
        String errMsg = bindingErrMsg(e.getBindingResult());
        return new Msg(AgentErrorCodeEnum.MISSING_PARAM_IS.getCode(), errMsg, null);
    }

    /**
     * 校验 除了requestbody注解方式的参数校验,对应的bindingresult为BeanPropertyBindingResult
     */
    @ExceptionHandler({BindException.class})
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public Msg handleBindException(BindException e) {
        String errMsg = bindingErrMsg(e.getBindingResult());
        return new Msg(AgentErrorCodeEnum.MISSING_PARAM_IS.getCode(), errMsg, null);
    }

    @ExceptionHandler({ConstraintViolationException.class})
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public Msg handleConstraintViolationException(ConstraintViolationException e) {
        StringBuilder stringBuilder = new StringBuilder();
        e.getConstraintViolations().forEach(constraintViolation ->
                stringBuilder.append(constraintViolation.getMessage()).append(";")
        );
        String errMsg = String.format(AgentErrorCodeEnum.MISSING_PARAM_IS.getMessage(), stringBuilder.toString());
        return new Msg(AgentErrorCodeEnum.MISSING_PARAM_IS.getCode(), errMsg, null);
    }

    private String bindingErrMsg(BindingResult bindingResult) {
        StringBuilder stringBuilder = new StringBuilder();
        bindingResult.getFieldErrors().forEach(o -> stringBuilder.append(o.getDefaultMessage()).append("|"));
        return String.format(AgentErrorCodeEnum.MISSING_PARAM_IS.getMessage(), stringBuilder.toString());
    }
}

