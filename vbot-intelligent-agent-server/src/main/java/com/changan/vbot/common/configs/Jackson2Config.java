package com.changan.vbot.common.configs;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Configuration
public class Jackson2Config {

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss") {
        //根据实际业务支持各种复杂格式的日期字符串。
        @Override
        public Date parse(String source) {
            try {
                if (StrUtil.isNotEmpty(source)) {
                    return super.parse(source);//支持解析指定pattern类型。
                }
            } catch (Exception e) {
                try {
                    if (StrUtil.isNotEmpty(source)) {
                        return new StdDateFormat().parse(source);//支持解析long类型的时间戳
                    }
                } catch (ParseException e1) {
                    throw new RuntimeException("日期格式非法：" + e);
                }
            }
            return null;
        }
    };


    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        //设置解析JSON工具类
        ObjectMapper objectMapper = new ObjectMapper();
        //设置解析日期的工具类
        objectMapper.setDateFormat(dateFormat);
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        //忽略未知属性 防止解析报错
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        jsonConverter.setObjectMapper(objectMapper);
        List<MediaType> list = new ArrayList<>();
        list.add(MediaType.APPLICATION_JSON_UTF8);
        jsonConverter.setSupportedMediaTypes(list);
        return jsonConverter;
    }

}
