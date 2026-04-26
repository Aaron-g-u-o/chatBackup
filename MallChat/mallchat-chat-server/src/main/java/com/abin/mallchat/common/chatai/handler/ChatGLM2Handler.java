package com.abin.mallchat.common.chatai.handler;

import cn.hutool.http.HttpResponse;
import com.abin.mallchat.common.chat.domain.entity.Message;
import com.abin.mallchat.common.chat.domain.entity.msg.MessageExtra;
import com.abin.mallchat.common.chatai.domain.ChatGPTContext;
import com.abin.mallchat.common.chatai.domain.ChatGPTMsg;
import com.abin.mallchat.common.chatai.domain.builder.ChatGPTMsgBuilder;
import com.abin.mallchat.common.chatai.properties.ChatGLM2Properties;
import com.abin.mallchat.common.chatai.service.AIContextService;
import com.abin.mallchat.common.chatai.utils.ChatGLM2Utils;
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
public class ChatGLM2Handler extends AbstractChatAIHandler {

    private static final String CHAT_GLM2_FREQUENCY_PREFIX = "ChatGLM2Handler";
    private static final int AI_TYPE = 1;

    private static final List<String> ERROR_MSG = Arrays.asList(
            "还摸鱼呢？你不下班我还要下班呢。。。。",
            "没给钱，矿工了。。。。",
            "服务器被你们玩儿坏了。。。。",
            "你们这群人，我都不想理你们了。。。。",
            "艾特我那是另外的价钱。。。。",
            "得加钱");

    private static final Random RANDOM = new Random();
    private static String AI_NAME;

    @Autowired
    private ChatGLM2Properties glm2Properties;

    @Autowired
    private AIContextService aiContextService;

    @Override
    protected void init() {
        super.init();
        if (isUse()) {
            log.info("=== GLM Handler Initializing ===");
            log.info("GLM Config - use: {}, model: {}, apiKey: {}, AIUserId: {}", 
                glm2Properties.isUse(), 
                glm2Properties.getModel(), 
                glm2Properties.getApiKey() != null ? "****(已配置)" : "未配置",
                glm2Properties.getAIUserId());
            
            if (glm2Properties.getAIUserId() == null) {
                log.error("GLM AIUserId is not configured! Please set mallchat.chatglm2.uid");
                return;
            }
            
            UserInfoResp userInfo = userService.getUserInfo(glm2Properties.getAIUserId());
            if (userInfo == null) {
                log.error("根据AIUserId:{} 找不到用户信息", glm2Properties.getAIUserId());
                throw new RuntimeException("根据AIUserId找不到用户信息");
            }
            if (StringUtils.isBlank(userInfo.getName())) {
                log.warn("根据AIUserId:{} 找到的用户信息没有name", glm2Properties.getAIUserId());
                throw new RuntimeException("根据AIUserId: " + glm2Properties.getAIUserId() + " 找到的用户没有名字");
            }
            AI_NAME = userInfo.getName();
            log.info("ChatGLM Handler initialized - AI_NAME: {}, Model: {}", AI_NAME, glm2Properties.getModel());
            log.info("=== GLM Handler Ready ===");
        } else {
            log.info("GLM Handler is disabled");
        }
    }

    @Override
    protected boolean isUse() {
        return glm2Properties.isUse();
    }

    @Override
    public Long getChatAIUserId() {
        return glm2Properties.getAIUserId();
    }

    @Override
    protected String doChat(Message message) {
        Long uid = message.getFromUid();
        
        try {
            FrequencyControlDTO frequencyControlDTO = new FrequencyControlDTO();
            frequencyControlDTO.setKey(CHAT_GLM2_FREQUENCY_PREFIX + ":" + uid);
            frequencyControlDTO.setUnit(TimeUnit.MINUTES);
            frequencyControlDTO.setCount(glm2Properties.getLimit());
            frequencyControlDTO.setTime(1);
            
            return FrequencyControlUtil.executeWithFrequencyControl(
                TOTAL_COUNT_WITH_IN_FIX_TIME_FREQUENCY_CONTROLLER, 
                frequencyControlDTO, 
                () -> sendRequestToGLM(message)
            );
        } catch (FrequencyControlException e) {
            return "你今天问太多次了，明天再来吧~";
        } catch (Throwable e) {
            log.error("GLM doChat error:", e);
            return "系统开小差啦~~";
        }
    }

    private String sendRequestToGLM(Message message) {
        String prompt = message.getContent().replace("@" + AI_NAME, "").trim();
        Long uid = message.getFromUid();
        Long roomId = message.getRoomId();

        ChatGPTContext context = buildContext(uid, roomId, prompt);
        context = tailorContext(context);
        
        log.info("发送GLM请求: uid={}, roomId={}, sessionId={}, msgCount={}", 
            uid, roomId, context.getSessionId(), context.getMsg().size());

        String text;
        try {
            saveUserMessage(context, prompt);

            ChatGLM2Utils glmUtils = ChatGLM2Utils.create()
                    .url(glm2Properties.getUrl())
                    .apiKey(glm2Properties.getApiKey())
                    .model(glm2Properties.getModel())
                    .maxTokens(glm2Properties.getMaxTokens())
                    .temperature(glm2Properties.getTemperature())
                    .timeout(glm2Properties.getTimeout());
            
            for (ChatGPTMsg msg : context.getMsg()) {
                glmUtils.addMessage(msg.getRole(), msg.getContent());
            }
            
            HttpResponse response = glmUtils.send();
            text = ChatGLM2Utils.parseText(response);

            context.addMsg(ChatGPTMsgBuilder.assistantMsg(text));

            saveAssistantMessage(context, text);

            cacheContext(context);
            
            log.info("GLM Response: {}", text);
            
        } catch (Exception e) {
            log.warn("GLM doChat warn:", e);
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
        int tokenBudget = glm2Properties.getMaxTokens() - 500;
        
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
        if (!glm2Properties.isUse()) {
            log.debug("GLM handler disabled");
            return false;
        }
        
        if (StringUtils.isBlank(glm2Properties.getApiKey())) {
            log.warn("GLM API Key is not configured! Please set mallchat.chatglm2.apiKey");
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
        if (!extra.getAtUidList().contains(glm2Properties.getAIUserId())) {
            log.debug("Message not for GLM bot. AtUids: {}, BotId: {}", extra.getAtUidList(), glm2Properties.getAIUserId());
            return false;
        }

        if (StringUtils.isBlank(message.getContent())) {
            return false;
        }
        boolean supported = StringUtils.contains(message.getContent(), "@" + AI_NAME)
                && StringUtils.isNotBlank(message.getContent().replace(AI_NAME, "").trim());
        log.info("GLM supports check: content={}, AI_NAME={}, supported={}", message.getContent(), AI_NAME, supported);
        return supported;
    }
}
