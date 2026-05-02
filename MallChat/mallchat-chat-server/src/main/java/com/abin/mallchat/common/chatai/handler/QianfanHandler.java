package com.abin.mallchat.common.chatai.handler;

import cn.hutool.http.HttpResponse;
import com.abin.mallchat.common.chat.domain.entity.Message;
import com.abin.mallchat.common.chat.domain.entity.msg.MessageExtra;
import com.abin.mallchat.common.chatai.domain.ChatGPTContext;
import com.abin.mallchat.common.chatai.domain.ChatGPTMsg;
import com.abin.mallchat.common.chatai.domain.builder.ChatGPTMsgBuilder;
import com.abin.mallchat.common.chatai.properties.QianfanProperties;
import com.abin.mallchat.common.chatai.service.AIContextService;
import com.abin.mallchat.common.chatai.utils.QianfanUtils;
import com.abin.mallchat.common.chatai.utils.ChatGPTUtils;
import com.abin.mallchat.common.common.constant.RedisKey;
import com.abin.mallchat.common.common.domain.dto.FrequencyControlDTO;
import com.abin.mallchat.common.common.exception.FrequencyControlException;
import com.abin.mallchat.common.common.service.frequencycontrol.FrequencyControlUtil;
import com.abin.mallchat.common.common.utils.RedisUtils;
import com.abin.mallchat.common.user.domain.vo.response.user.UserInfoResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.abin.mallchat.common.common.constant.RedisKey.USER_CHAT_CONTEXT;
import static com.abin.mallchat.common.common.service.frequencycontrol.FrequencyControlStrategyFactory.TOTAL_COUNT_WITH_IN_FIX_TIME_FREQUENCY_CONTROLLER;

@Slf4j
@Component
public class QianfanHandler extends AbstractChatAIHandler {

    private static final String QIANFAN_FREQUENCY_PREFIX = "QianfanHandler";
    private static final int AI_TYPE = 2;

    private static final List<String> ERROR_MSG = Arrays.asList(
            "百度AI正在休息中，稍后再试~",
            "文心一言思考太投入了，请稍等~",
            "服务器开小差了，再来一次吧~",
            "AI正在充电，马上回来~");

    private static final Random RANDOM = new Random();
    private static String AI_NAME;

    @Autowired
    private QianfanProperties qianfanProperties;

    @Autowired
    private AIContextService aiContextService;

    @Override
    protected void init() {
        super.init();
        if (isUse()) {
            log.info("=== Qianfan Handler Initializing ===");
            log.info("Qianfan Config - use: {}, model: {}, apiKey: {}, AIUserId: {}", 
                qianfanProperties.isUse(), 
                qianfanProperties.getModel(), 
                qianfanProperties.getApiKey() != null ? "****(已配置)" : "未配置",
                qianfanProperties.getAIUserId());
            
            if (qianfanProperties.getAIUserId() == null) {
                log.error("Qianfan AIUserId is not configured! Please set mallchat.qianfan.uid");
                return;
            }
            
            UserInfoResp userInfo = userService.getUserInfo(qianfanProperties.getAIUserId());
            if (userInfo == null) {
                log.error("根据AIUserId:{} 找不到用户信息", qianfanProperties.getAIUserId());
                throw new RuntimeException("根据AIUserId找不到用户信息");
            }
            if (StringUtils.isBlank(userInfo.getName())) {
                log.warn("根据AIUserId:{} 找到的用户信息没有name", qianfanProperties.getAIUserId());
                throw new RuntimeException("根据AIUserId: " + qianfanProperties.getAIUserId() + " 找到的用户没有名字");
            }
            AI_NAME = userInfo.getName();
            log.info("Qianfan Handler initialized - AI_NAME: {}, Model: {}", AI_NAME, qianfanProperties.getModel());
            log.info("=== Qianfan Handler Ready ===");
        } else {
            log.info("Qianfan Handler is disabled");
        }
    }

    @Override
    protected boolean isUse() {
        return qianfanProperties.isUse();
    }

    @Override
    public Long getChatAIUserId() {
        return qianfanProperties.getAIUserId();
    }

    @Override
    protected String doChat(Message message) {
        Long uid = message.getFromUid();
        
        try {
            FrequencyControlDTO frequencyControlDTO = new FrequencyControlDTO();
            frequencyControlDTO.setKey(QIANFAN_FREQUENCY_PREFIX + ":" + uid);
            frequencyControlDTO.setUnit(TimeUnit.MINUTES);
            frequencyControlDTO.setCount(qianfanProperties.getLimit());
            frequencyControlDTO.setTime(1);
            
            return FrequencyControlUtil.executeWithFrequencyControl(
                TOTAL_COUNT_WITH_IN_FIX_TIME_FREQUENCY_CONTROLLER, 
                frequencyControlDTO, 
                () -> sendRequestToQianfan(message)
            );
        } catch (FrequencyControlException e) {
            return "你今天问太多次了，明天再来吧~";
        } catch (Throwable e) {
            log.error("Qianfan doChat error:", e);
            return "系统开小差啦~~";
        }
    }

    private String sendRequestToQianfan(Message message) {
        String prompt = message.getContent().replace("@" + AI_NAME, "").trim();
        Long uid = message.getFromUid();
        Long roomId = message.getRoomId();

        ChatGPTContext context = buildContext(uid, roomId, prompt);
        context = tailorContext(context);
        
        log.info("发送Qianfan请求: uid={}, roomId={}, sessionId={}, msgCount={}", 
            uid, roomId, context.getSessionId(), context.getMsg().size());

        String text;
        try {
            saveUserMessage(context, prompt);

            QianfanUtils qianfanUtils = QianfanUtils.create()
                    .apiKey(qianfanProperties.getApiKey())
                    .model(qianfanProperties.getModel())
                    .maxTokens(qianfanProperties.getMaxTokens())
                    .temperature(qianfanProperties.getTemperature())
                    .timeout(qianfanProperties.getTimeout());
            
            for (ChatGPTMsg msg : context.getMsg()) {
                qianfanUtils.addMessage(msg.getRole(), msg.getContent());
            }
            
            HttpResponse response = qianfanUtils.send();
            text = QianfanUtils.parseText(response);

            context.addMsg(ChatGPTMsgBuilder.assistantMsg(text));

            saveAssistantMessage(context, text);

            cacheContext(context);
            
            log.info("Qianfan Response: {}", text != null && text.length() > 100 ? text.substring(0, 100) + "..." : text);
            
        } catch (Exception e) {
            log.error("Qianfan doChat error: {}", e.getMessage(), e);
            text = getErrorText();
        }
        return text;
    }

    private ChatGPTContext buildContext(Long uid, Long roomId, String prompt) {
        ChatGPTContext cachedContext = RedisUtils.get(
            RedisKey.getKey(USER_CHAT_CONTEXT, uid, roomId), ChatGPTContext.class);

        if (cachedContext != null && cachedContext.getSessionId() != null) {
            log.debug("Redis缓存命中: uid={}, roomId={}, sessionId={}", uid, roomId, cachedContext.getSessionId());
            cachedContext.addMsg(ChatGPTMsgBuilder.userMsg(prompt));
            return cachedContext;
        }

        log.info("Redis缓存未命中，从数据库重建上下文: uid={}, roomId={}", uid, roomId);
        ChatGPTContext context = aiContextService.buildContext(uid, roomId, AI_TYPE, prompt);
        return context;
    }

    private ChatGPTContext tailorContext(ChatGPTContext context) {
        List<ChatGPTMsg> msg = context.getMsg();
        Integer totalTokens = ChatGPTUtils.countTokens(msg);
        int tokenBudget = qianfanProperties.getMaxTokens() - 500;
        
        if (totalTokens < tokenBudget) {
            return context;
        }
        
        while (msg.size() > 2 && ChatGPTUtils.countTokens(msg) >= tokenBudget) {
            msg.remove(1);
        }
        
        return context;
    }

    private void saveUserMessage(ChatGPTContext context, String prompt) {
        if (context.getSessionId() == null) return;
        try {
            int tokenCount = aiContextService.calculateTokenCount(prompt);
            aiContextService.saveMessage(context.getSessionId(), "user", prompt, tokenCount, null);
            log.debug("保存用户消息: sessionId={}, tokens={}", context.getSessionId(), tokenCount);
        } catch (Exception e) {
            log.warn("保存用户消息失败", e);
        }
    }

    private void saveAssistantMessage(ChatGPTContext context, String text) {
        if (context.getSessionId() == null || StringUtils.isBlank(text)) return;
        try {
            int tokenCount = aiContextService.calculateTokenCount(text);
            aiContextService.saveMessage(context.getSessionId(), "assistant", text, tokenCount, null);
            log.debug("保存AI回复: sessionId={}, tokens={}", context.getSessionId(), tokenCount);
        } catch (Exception e) {
            log.warn("保存AI回复失败", e);
        }
    }

    private void cacheContext(ChatGPTContext context) {
        RedisUtils.set(
            RedisKey.getKey(USER_CHAT_CONTEXT, context.getUid(), context.getRoomId()),
            context, 30L, TimeUnit.MINUTES);
    }

    private static String getErrorText() {
        int index = RANDOM.nextInt(ERROR_MSG.size());
        return ERROR_MSG.get(index);
    }

    @Override
    protected boolean supports(Message message) {
        if (!qianfanProperties.isUse()) {
            log.debug("Qianfan handler disabled");
            return false;
        }
        
        if (StringUtils.isBlank(qianfanProperties.getApiKey())) {
            log.warn("Qianfan API Key is not configured!");
            return false;
        }
        
        MessageExtra extra = message.getExtra();
        if (extra == null) {
            log.debug("Message extra is null");
            return false;
        }
        if (CollectionUtils.isEmpty(extra.getAtUidList())) {
            log.debug("No at list in message");
            return false;
        }
        if (!extra.getAtUidList().contains(qianfanProperties.getAIUserId())) {
            log.debug("Message not for Qianfan bot. AtUids: {}, BotId: {}", extra.getAtUidList(), qianfanProperties.getAIUserId());
            return false;
        }

        if (StringUtils.isBlank(message.getContent())) {
            return false;
        }
        boolean supported = StringUtils.contains(message.getContent(), "@" + AI_NAME)
                && StringUtils.isNotBlank(message.getContent().replace(AI_NAME, "").trim());
        log.info("Qianfan supports check: content={}, AI_NAME={}, supported={}", message.getContent(), AI_NAME, supported);
        return supported;
    }
}
