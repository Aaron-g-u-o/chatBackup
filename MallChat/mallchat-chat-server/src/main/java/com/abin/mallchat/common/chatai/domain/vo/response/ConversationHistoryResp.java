package com.abin.mallchat.common.chatai.domain.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ConversationHistoryResp {
    private Long sessionId;
    private String title;
    private Integer totalCount;
    private Integer page;
    private Integer pageSize;
    private List<MessageItem> messages;
    
    @Data
    public static class MessageItem {
        private Long id;
        private String role;
        private String content;
        private Integer tokenCount;
        private String intentTags;
        private Date createTime;
    }
}
