package com.abin.mallchat.common.discovery.domain.vo.response;

import lombok.Data;

@Data
public class TagResp {
    private Long id;
    private String name;
    private String category;
    private Double weight;
}
