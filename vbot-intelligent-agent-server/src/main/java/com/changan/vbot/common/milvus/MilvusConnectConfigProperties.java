package com.changan.vbot.common.milvus;

import lombok.Data;

@Data
public class MilvusConnectConfigProperties {
    private String uri;
    private String token;
    private String username;
    private String password;
    private String dbName;
}
