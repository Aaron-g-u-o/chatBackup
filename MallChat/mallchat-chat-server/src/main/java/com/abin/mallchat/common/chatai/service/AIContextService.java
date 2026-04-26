package com.abin.mallchat.common.chatai.service;

import com.abin.mallchat.common.chatai.domain.ChatGPTContext;
import com.abin.mallchat.common.chatai.domain.ChatGPTMsg;
import com.abin.mallchat.common.chatai.domain.entity.AIMessageHistory;
import com.abin.mallchat.common.chatai.domain.entity.AISession;
import com.abin.mallchat.common.chatai.domain.vo.request.ContextConfigReq;
import com.abin.mallchat.common.chatai.domain.vo.response.AIContextConfigResp;
import com.abin.mallchat.common.chatai.domain.vo.response.ConversationHistoryResp;
import com.abin.mallchat.common.chatai.domain.vo.response.UserIntentResp;

import java.util.List;

public interface AIContextService {
    
    AISession createOrGetSession(Long uid, Long roomId, Integer aiType);
    
    ChatGPTContext buildContext(Long uid, Long roomId, Integer aiType, String userMessage);
    
    void saveMessage(Long sessionId, String role, String content, int tokenCount, String intentTags);
    
    List<AIMessageHistory> getRecentMessages(Long sessionId, int limit);
    
    List<AIMessageHistory> getMessagesByIntent(Long sessionId, String intentType);
    
    UserIntentResp analyzeIntent(Long uid, Long sessionId, String message, String contextBefore);
    
    String getSystemPrompt(Long uid, Integer aiType);
    
    void updateSystemPrompt(Long uid, Integer aiType, String prompt);
    
    AIContextConfigResp getUserConfig(Long uid, Integer aiType);
    
    void updateUserConfig(Long uid, ContextConfigReq req);
    
    void prependSystemPrompt(Long uid, Integer aiType, String additionalPrompt);
    
    void clearContext(Long sessionId);
    
    void archiveSession(Long sessionId);
    
    ConversationHistoryResp getConversationHistory(Long sessionId, int page, int pageSize);
    
    int calculateTokenCount(String content);
    
    List<ChatGPTMsg> getContextMessagesForAI(Long uid, Long roomId, int maxTokens);
}
