package com.abin.mallchat.common.chatai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.abin.mallchat.common.chatai.domain.ChatGPTContext;
import com.abin.mallchat.common.chatai.domain.ChatGPTMsg;
import com.abin.mallchat.common.chatai.domain.builder.ChatGPTContextBuilder;
import com.abin.mallchat.common.chatai.domain.builder.ChatGPTMsgBuilder;
import com.abin.mallchat.common.chatai.domain.entity.AIMessageHistory;
import com.abin.mallchat.common.chatai.domain.entity.AISession;
import com.abin.mallchat.common.chatai.domain.entity.AIUserIntent;
import com.abin.mallchat.common.chatai.domain.vo.request.ContextConfigReq;
import com.abin.mallchat.common.chatai.domain.vo.response.AIContextConfigResp;
import com.abin.mallchat.common.chatai.domain.vo.response.ConversationHistoryResp;
import com.abin.mallchat.common.chatai.domain.vo.response.UserIntentResp;
import com.abin.mallchat.common.chatai.mapper.AIMessageHistoryMapper;
import com.abin.mallchat.common.chatai.mapper.AISessionMapper;
import com.abin.mallchat.common.chatai.mapper.AIUserIntentMapper;
import com.abin.mallchat.common.chatai.service.AIContextService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AIContextServiceImpl extends ServiceImpl<AISessionMapper, AISession> implements AIContextService {

    @Autowired
    private AIMessageHistoryMapper messageHistoryMapper;

    @Autowired
    private AIUserIntentMapper userIntentMapper;

    private static final int DEFAULT_CONTEXT_WINDOW = 10;
    private static final int MAX_CONTEXT_TOKENS = 2000;
    private static final double TOKEN_TO_CHAR_RATIO = 4;

    private static final String DEFAULT_SYSTEM_PROMPT =
        "你是语音交流平台系统的AI助手\n\n" +
        "【你的角色】\n" +
        "- 你是语音交流平台的智能助手，友好、专业、乐于助人\n" +
        "- 能用简洁清晰的语言回答问题\n\n" +
        "【回答规范】\n" +
        "- 回答控制在500字以内，简洁明了\n" +
        "- 使用中文回答，语言亲切自然\n" +
        "- 对于技术问题，提供准确、专业的解答\n" +
        "- 对于闲聊，保持友好和幽默感\n" +
        "- 如果不确定答案，诚实告知用户\n" +
        "- 注意关联用户之前的对话内容，保持上下文连贯\n\n" +
        "【重要】当用户提到\"上面说的\"、\"刚才那个\"、\"继续\"等指代词时，" +
        "请根据对话历史理解用户所指的内容，确保回答与上下文一致。";

    private static final String QIANFAN_SYSTEM_PROMPT =
        "你是语音交流平台的AI助手，由百度文心大模型驱动。\n\n" +
        "【你的角色】\n" +
        "- 你是语音交流平台的智能助手，友好、专业、乐于助人\n" +
        "- 由百度文心一言(ERNIE)提供技术支持\n" +
        "- 能用简洁清晰的语言回答问题\n\n" +
        "【回答规范】\n" +
        "- 回答控制在500字以内，简洁明了\n" +
        "- 使用中文回答，语言亲切自然\n" +
        "- 对于技术问题，提供准确、专业的解答\n" +
        "- 对于闲聊，保持友好和幽默感\n" +
        "- 如果不确定答案，诚实告知用户\n" +
        "- 注意关联用户之前的对话内容，保持上下文连贯\n\n" +
        "【重要】当用户提到\"上面说的\"、\"刚才那个\"、\"继续\"等指代词时，" +
        "请根据对话历史理解用户所指的内容，确保回答与上下文一致。";

    @Override
    @Transactional
    public AISession createOrGetSession(Long uid, Long roomId, Integer aiType) {
        AISession session = lambdaQuery()
                .eq(AISession::getUid, uid)
                .eq(AISession::getRoomId, roomId)
                .eq(AISession::getAiType, aiType)
                .eq(AISession::getIsActive, 1)
                .orderByDesc(AISession::getLastMessageTime)
                .last("LIMIT 1")
                .one();

        if (session == null) {
            session = new AISession();
            session.setUid(uid);
            session.setRoomId(roomId);
            session.setAiType(aiType);
            session.setTitle("新对话");
            session.setSystemPrompt(getSystemPrompt(uid, aiType));
            session.setContextWindow(DEFAULT_CONTEXT_WINDOW);
            session.setTotalTokens(0);
            session.setMessageCount(0);
            session.setIsActive(1);
            session.setLastMessageTime(new Date());
            save(session);
            log.info("创建新AI会话: uid={}, roomId={}, aiType={}, sessionId={}", uid, roomId, aiType, session.getId());
        } else {
            session.setLastMessageTime(new Date());
            updateById(session);
            log.debug("复用已有AI会话: uid={}, roomId={}, sessionId={}", uid, roomId, session.getId());
        }

        return session;
    }

    @Override
    public ChatGPTContext buildContext(Long uid, Long roomId, Integer aiType, String userMessage) {
        AISession session = createOrGetSession(uid, roomId, aiType);

        ChatGPTContext context = new ChatGPTContext();
        context.setSessionId(session.getId());
        context.setUid(uid);
        context.setRoomId(roomId);
        context.setModel(getModelByType(aiType));

        List<ChatGPTMsg> messages = new ArrayList<>();

        String systemPrompt = buildEffectiveSystemPrompt(session.getSystemPrompt(), userMessage, uid);
        if (StrUtil.isNotBlank(systemPrompt)) {
            ChatGPTMsg systemMsg = new ChatGPTMsg();
            systemMsg.setRole("system");
            systemMsg.setContent(systemPrompt);
            messages.add(systemMsg);
        }

        int windowSize = session.getContextWindow() != null ? session.getContextWindow() : DEFAULT_CONTEXT_WINDOW;
        List<AIMessageHistory> histories = getRecentMessages(session.getId(), windowSize * 2);
        Collections.reverse(histories);

        int usedTokens = 0;
        for (AIMessageHistory history : histories) {
            int msgTokens = history.getTokenCount() != null ? history.getTokenCount() : calculateTokenCount(history.getContent());
            if (usedTokens + msgTokens > MAX_CONTEXT_TOKENS) {
                log.debug("上下文Token超限，截断历史: sessionId={}, usedTokens={}, msgTokens={}", 
                    session.getId(), usedTokens, msgTokens);
                break;
            }
            ChatGPTMsg msg = new ChatGPTMsg();
            msg.setRole(history.getRole());
            msg.setContent(history.getContent());
            messages.add(msg);
            usedTokens += msgTokens;
        }

        ChatGPTMsg userMsg = new ChatGPTMsg();
        userMsg.setRole("user");
        userMsg.setContent(userMessage);
        messages.add(userMsg);

        context.setMsg(messages);

        log.info("构建上下文完成: uid={}, roomId={}, sessionId={}, systemMsg=1, historyMsg={}, userMsg=1, totalMsg={}", 
            uid, roomId, session.getId(), messages.size() - 2, messages.size());

        return context;
    }

    private String buildEffectiveSystemPrompt(String basePrompt, String userMessage, Long uid) {
        StringBuilder effectivePrompt = new StringBuilder();

        if (StrUtil.isNotBlank(basePrompt)) {
            effectivePrompt.append(basePrompt).append("\n\n");
        }

        effectivePrompt.append("当前时间: ").append(new Date()).append("\n");

        if (StrUtil.isNotBlank(userMessage)) {
            String contextHint = inferContextHint(userMessage);
            if (StrUtil.isNotBlank(contextHint)) {
                effectivePrompt.append("\n").append(contextHint);
            }
        }

        return effectivePrompt.toString();
    }

    private String inferContextHint(String message) {
        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("上面说的") || lowerMessage.contains("刚才那个") || 
            lowerMessage.contains("继续") || lowerMessage.contains("还有呢") ||
            lowerMessage.contains("那个") || lowerMessage.contains("它") ||
            lowerMessage.contains("这个") || lowerMessage.contains("之前")) {
            return "【上下文提示】用户正在引用之前的对话内容，请仔细回顾历史消息，确保回答与之前的话题保持一致。";
        }

        if (lowerMessage.contains("代码") || lowerMessage.contains("function") || lowerMessage.contains("class ")) {
            return "【上下文提示】用户可能需要代码示例，请确保代码准确且完整。";
        }
        if (lowerMessage.contains("怎么") || lowerMessage.contains("如何") || lowerMessage.contains("?") || lowerMessage.contains("？")) {
            return "【上下文提示】用户在寻求帮助或解释，请提供清晰、分步骤的说明。";
        }
        if (lowerMessage.contains("比较") || lowerMessage.contains("区别") || lowerMessage.contains("vs")) {
            return "【上下文提示】用户想要对比分析，请从多角度进行分析。";
        }

        return null;
    }

    @Override
    @Transactional
    public void saveMessage(Long sessionId, String role, String content, int tokenCount, String intentTags) {
        AISession session = getById(sessionId);
        if (session == null) {
            log.warn("保存消息失败: sessionId={} 不存在", sessionId);
            return;
        }

        AIMessageHistory history = new AIMessageHistory();
        history.setSessionId(sessionId);
        history.setRole(role);
        history.setContent(content);
        history.setTokenCount(tokenCount);
        history.setIntentTags(intentTags);
        messageHistoryMapper.insert(history);

        session.setMessageCount(session.getMessageCount() + 1);
        session.setTotalTokens(session.getTotalTokens() + tokenCount);
        session.setLastMessageTime(new Date());

        if ("user".equals(role) && session.getMessageCount() <= 2) {
            String title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
            session.setTitle(title);
        }

        updateById(session);

        trimOldMessages(sessionId, session.getContextWindow());
    }

    private void trimOldMessages(Long sessionId, int maxWindow) {
        long count = messageHistoryMapper.selectCount(
            new LambdaQueryWrapper<AIMessageHistory>()
                .eq(AIMessageHistory::getSessionId, sessionId)
        );

        if (count > maxWindow * 2) {
            List<AIMessageHistory> oldMessages = messageHistoryMapper.selectList(
                new LambdaQueryWrapper<AIMessageHistory>()
                    .eq(AIMessageHistory::getSessionId, sessionId)
                    .orderByAsc(AIMessageHistory::getCreateTime)
                    .last("LIMIT " + (count - maxWindow))
            );

            if (CollUtil.isNotEmpty(oldMessages)) {
                List<Long> ids = oldMessages.stream().map(AIMessageHistory::getId).collect(Collectors.toList());
                messageHistoryMapper.deleteBatchIds(ids);
                log.info("清理旧消息: sessionId={}, count={}", sessionId, ids.size());
            }
        }
    }

    @Override
    public List<AIMessageHistory> getRecentMessages(Long sessionId, int limit) {
        return messageHistoryMapper.selectList(
            new LambdaQueryWrapper<AIMessageHistory>()
                .eq(AIMessageHistory::getSessionId, sessionId)
                .orderByDesc(AIMessageHistory::getCreateTime)
                .last("LIMIT " + limit)
        );
    }

    @Override
    public List<AIMessageHistory> getMessagesByIntent(Long sessionId, String intentType) {
        return messageHistoryMapper.selectList(
            new LambdaQueryWrapper<AIMessageHistory>()
                .eq(AIMessageHistory::getSessionId, sessionId)
                .like(AIMessageHistory::getIntentTags, intentType)
                .orderByDesc(AIMessageHistory::getCreateTime)
                .last("LIMIT 20")
        );
    }

    @Override
    public UserIntentResp analyzeIntent(Long uid, Long sessionId, String message, String contextBefore) {
        UserIntentResp resp = new UserIntentResp();

        String intentType = classifyIntent(message);
        resp.setIntentType(intentType);

        String keywords = extractKeywords(message);
        resp.setIntentKeywords(keywords);

        double confidence = calculateConfidence(message, keywords);
        resp.setConfidence(confidence);

        resp.setContextBefore(contextBefore);

        String entities = extractEntities(message);
        resp.setEntities(entities);

        boolean followupNeeded = needsFollowup(intentType, message);
        resp.setFollowupNeeded(followupNeeded);

        if (sessionId != null) {
            AIUserIntent intent = new AIUserIntent();
            intent.setUid(uid);
            intent.setSessionId(sessionId);
            intent.setIntentType(intentType);
            intent.setIntentKeywords(keywords);
            intent.setConfidence(confidence);
            intent.setContextBefore(contextBefore);
            intent.setEntities(entities);
            intent.setFollowupNeeded(followupNeeded ? 1 : 0);

            userIntentMapper.insert(intent);
        }

        return resp;
    }

    private String classifyIntent(String message) {
        String lower = message.toLowerCase();

        if (lower.contains("怎么") || lower.contains("如何") || lower.contains("?") || lower.contains("请帮我") || lower.contains("能不能")) {
            return "question";
        }
        if (lower.contains("帮我") || lower.contains("帮我做") || lower.contains("写个") || lower.contains("生成")) {
            return "task";
        }
        if (lower.contains("哈哈") || lower.contains("你好") || lower.contains("在吗") || lower.contains("嗨")) {
            return "greeting";
        }
        if (lower.contains("不好") || lower.contains("不对") || lower.contains("错了") || lower.contains("不要")) {
            return "feedback";
        }

        return "chat";
    }

    private String extractKeywords(String message) {
        Set<String> keywords = new HashSet<>();

        String[] patterns = {"编程", "代码", "java", "python", "前端", "后端", "数据库", "算法", "AI", "机器学习",
            "设计", "产品", "运营", "创业", "投资", "学习", "工作", "生活", "游戏", "音乐"};

        for (String pattern : patterns) {
            if (message.toLowerCase().contains(pattern.toLowerCase())) {
                keywords.add(pattern);
            }
        }

        return String.join(",", keywords);
    }

    private double calculateConfidence(String message, String keywords) {
        double confidence = 0.5;

        if (keywords.split(",").length > 0) {
            confidence += 0.1;
        }

        if (message.length() > 10 && message.length() < 500) {
            confidence += 0.1;
        }

        if (message.contains("?") || message.contains("？")) {
            confidence += 0.15;
        }

        return Math.min(1.0, confidence);
    }

    private String extractEntities(String message) {
        List<String> entities = new ArrayList<>();

        if (message.contains("Java") || message.contains("java")) entities.add("Java");
        if (message.contains("Python") || message.contains("python")) entities.add("Python");
        if (message.contains("JavaScript") || message.contains("JS")) entities.add("JavaScript");
        if (message.contains("Vue") || message.contains("React")) entities.add("Frontend");
        if (message.contains("Spring") || message.contains("Django")) entities.add("Framework");

        return entities.isEmpty() ? "[]" : "[\"" + String.join("\",\"", entities) + "\"]";
    }

    private boolean needsFollowup(String intentType, String message) {
        if ("question".equals(intentType) && (message.contains("?") || message.contains("？"))) {
            return true;
        }
        if ("task".equals(intentType) && message.length() < 20) {
            return true;
        }
        return false;
    }

    @Override
    public String getSystemPrompt(Long uid, Integer aiType) {
        if (aiType == null) {
            return DEFAULT_SYSTEM_PROMPT;
        }
        switch (aiType) {
            case 2:
                return QIANFAN_SYSTEM_PROMPT;
            default:
                return DEFAULT_SYSTEM_PROMPT;
        }
    }

    @Override
    public void updateSystemPrompt(Long uid, Integer aiType, String prompt) {
        AISession session = lambdaQuery()
                .eq(AISession::getUid, uid)
                .eq(AISession::getAiType, aiType)
                .eq(AISession::getIsActive, 1)
                .orderByDesc(AISession::getLastMessageTime)
                .last("LIMIT 1")
                .one();

        if (session != null) {
            session.setSystemPrompt(prompt);
            updateById(session);
        }
    }

    @Override
    public AIContextConfigResp getUserConfig(Long uid, Integer aiType) {
        AIContextConfigResp resp = new AIContextConfigResp();
        resp.setUid(uid);
        resp.setAiType(aiType);
        resp.setMaxContextWindow(DEFAULT_CONTEXT_WINDOW);
        resp.setEnableIntentTracking(true);
        resp.setEnableRelevanceFilter(true);
        resp.setRelevanceThreshold(0.3);
        resp.setDiversityRatio(0.2);
        return resp;
    }

    @Override
    public void updateUserConfig(Long uid, ContextConfigReq req) {
        log.info("Updated context config for uid={}, config={}", uid, req);
    }

    @Override
    public void prependSystemPrompt(Long uid, Integer aiType, String additionalPrompt) {
        AISession session = lambdaQuery()
                .eq(AISession::getUid, uid)
                .eq(AISession::getAiType, aiType)
                .eq(AISession::getIsActive, 1)
                .orderByDesc(AISession::getLastMessageTime)
                .last("LIMIT 1")
                .one();

        if (session != null) {
            String currentPrompt = session.getSystemPrompt();
            String newPrompt = additionalPrompt + "\n\n" + currentPrompt;
            session.setSystemPrompt(newPrompt);
            updateById(session);
        }
    }

    @Override
    public void clearContext(Long sessionId) {
        messageHistoryMapper.delete(
            new LambdaQueryWrapper<AIMessageHistory>()
                .eq(AIMessageHistory::getSessionId, sessionId)
        );

        AISession session = getById(sessionId);
        if (session != null) {
            session.setMessageCount(0);
            session.setTotalTokens(0);
            updateById(session);
        }

        log.info("清理上下文: sessionId={}", sessionId);
    }

    @Override
    public void archiveSession(Long sessionId) {
        AISession session = getById(sessionId);
        if (session != null) {
            session.setIsActive(0);
            updateById(session);
            log.info("归档会话: sessionId={}", sessionId);
        }
    }

    @Override
    public ConversationHistoryResp getConversationHistory(Long sessionId, int page, int pageSize) {
        ConversationHistoryResp resp = new ConversationHistoryResp();

        AISession session = getById(sessionId);
        if (session == null) return resp;

        resp.setSessionId(sessionId);
        resp.setTitle(session.getTitle());
        resp.setPage(page);
        resp.setPageSize(pageSize);

        long total = messageHistoryMapper.selectCount(
            new LambdaQueryWrapper<AIMessageHistory>()
                .eq(AIMessageHistory::getSessionId, sessionId)
        );
        resp.setTotalCount((int) total);

        int offset = (page - 1) * pageSize;
        List<AIMessageHistory> histories = messageHistoryMapper.selectList(
            new LambdaQueryWrapper<AIMessageHistory>()
                .eq(AIMessageHistory::getSessionId, sessionId)
                .orderByDesc(AIMessageHistory::getCreateTime)
                .last("LIMIT " + pageSize + " OFFSET " + offset)
        );

        List<ConversationHistoryResp.MessageItem> items = histories.stream()
            .map(h -> {
                ConversationHistoryResp.MessageItem item = new ConversationHistoryResp.MessageItem();
                item.setId(h.getId());
                item.setRole(h.getRole());
                item.setContent(h.getContent());
                item.setTokenCount(h.getTokenCount());
                item.setIntentTags(h.getIntentTags());
                item.setCreateTime(h.getCreateTime());
                return item;
            })
            .collect(Collectors.toList());

        resp.setMessages(items);
        return resp;
    }

    @Override
    public int calculateTokenCount(String content) {
        if (content == null || content.isEmpty()) return 0;
        return (int) Math.ceil(content.length() / TOKEN_TO_CHAR_RATIO);
    }

    @Override
    public List<ChatGPTMsg> getContextMessagesForAI(Long uid, Long roomId, int maxTokens) {
        AISession session = lambdaQuery()
                .eq(AISession::getUid, uid)
                .eq(AISession::getRoomId, roomId)
                .eq(AISession::getIsActive, 1)
                .orderByDesc(AISession::getLastMessageTime)
                .last("LIMIT 1")
                .one();

        if (session == null) return new ArrayList<>();

        int windowSize = session.getContextWindow() != null ? session.getContextWindow() : DEFAULT_CONTEXT_WINDOW;
        List<AIMessageHistory> histories = getRecentMessages(session.getId(), windowSize * 2);

        Collections.reverse(histories);

        List<ChatGPTMsg> messages = new ArrayList<>();
        int usedTokens = 0;

        for (AIMessageHistory history : histories) {
            if (usedTokens + history.getTokenCount() > maxTokens) {
                break;
            }

            ChatGPTMsg msg = new ChatGPTMsg();
            msg.setRole(history.getRole());
            msg.setContent(history.getContent());
            messages.add(msg);
            usedTokens += history.getTokenCount();
        }

        return messages;
    }

    private String getModelByType(Integer aiType) {
        if (aiType == null || aiType == 0) {
            return "gpt-3.5-turbo";
        } else if (aiType == 1) {
            return "glm-4-flash";
        }
        return "gpt-3.5-turbo";
    }
}
