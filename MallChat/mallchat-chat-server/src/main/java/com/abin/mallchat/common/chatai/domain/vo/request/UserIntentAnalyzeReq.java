package com.abin.mallchat.common.chatai.domain.vo.request;

import lombok.Data;

@Data
public class UserIntentAnalyzeReq {
    private Long sessionId;
    private String message;
    private String contextBefore;
}
