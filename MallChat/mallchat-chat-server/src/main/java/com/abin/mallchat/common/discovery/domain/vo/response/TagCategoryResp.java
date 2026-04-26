package com.abin.mallchat.common.discovery.domain.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class TagCategoryResp {
    private String category;
    private List<TagResp> tags;
}
