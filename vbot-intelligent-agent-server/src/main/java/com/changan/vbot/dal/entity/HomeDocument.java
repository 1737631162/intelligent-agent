package com.changan.vbot.dal.entity;

import com.google.gson.JsonObject;
import lombok.Data;

import java.util.List;

@Data
public class HomeDocument {

    private Long id;

    private List<Float> embedding;

    private String content;

    private Float score;

    private JsonObject metadata;

}
