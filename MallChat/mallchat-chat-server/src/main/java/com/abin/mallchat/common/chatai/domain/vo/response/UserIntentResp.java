package com.abin.mallchat.common.chatai.domain.vo.response;

import lombok.Data;

@Data
public class UserIntentResp {
    private String intentType;
    private String intentKeywords;
    private Double confidence;
    private String contextBefore;
    private String entities;
    private Boolean followupNeeded;
}
