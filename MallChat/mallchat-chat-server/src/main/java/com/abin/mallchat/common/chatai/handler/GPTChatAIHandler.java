package com.abin.mallchat.common.chatai.handler;

import com.abin.mallchat.common.chat.domain.entity.Message;
import com.abin.mallchat.common.chat.domain.entity.msg.MessageExtra;
import com.abin.mallchat.common.chatai.domain.ChatGPTContext;
import com.abin.mallchat.common.chatai.domain.ChatGPTMsg;
import com.abin.mallchat.common.chatai.domain.builder.ChatGPTMsgBuilder;
import com.abin.mallchat.common.chatai.properties.ChatGPTProperties;
import com.abin.mallchat.common.chatai.service.AIContextService;
import com.abin.mallchat.common.chatai.utils.ChatGPTUtils;
import com.abin.mallchat.common.common.constant.RedisKey;
import com.abin.mallchat.common.common.domain.dto.FrequencyControlDTO;
import com.abin.mallchat.common.common.exception.FrequencyControlException;
import com.abin.mallchat.common.common.service.frequencycontrol.FrequencyControlUtil;
import com.abin.mallchat.common.common.utils.DateUtils;
import com.abin.mallchat.common.common.utils.RedisUtils;
import com.abin.mallchat.common.user.domain.vo.response.user.UserInfoResp;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.abin.mallchat.common.common.constant.RedisKey.USER_CHAT_CONTEXT;
import static com.abin.mallchat.common.common.service.frequencycontrol.FrequencyControlStrategyFactory.TOTAL_COUNT_WITH_IN_FIX_TIME_FREQUENCY_CONTROLLER;

@Slf4j
@Component
public class GPTChatAIHandler extends AbstractChatAIHandler {

    private static final String CHAT_FREQUENCY_PREFIX = "GPTChatAIHandler";

    @Autowired
    private ChatGPTProperties chatGPTProperties;

    @Autowired
    private AIContextService aiContextService;

    private static String AI_NAME;

    @Override
    protected void init() {
        super.init();
        if (isUse()) {
            UserInfoResp userInfo = userService.getUserInfo(chatGPTProperties.getAIUserId());
            if (userInfo == null) {
                log.error("根据AIUserId:{} 找不到用户信息", chatGPTProperties.getAIUserId());
                throw new RuntimeException("根据AIUserId: " + chatGPTProperties.getAIUserId() + " 找不到用户信息");
            }
            if (StringUtils.isBlank(userInfo.getName())) {
                log.warn("根据AIUserId:{} 找到的用户信息没有name", chatGPTProperties.getAIUserId());
                throw new RuntimeException("根据AIUserId: " + chatGPTProperties.getAIUserId() + " 找到的用户没有名字");
            }
            AI_NAME = userInfo.getName();
        }
    }

    @Override
    protected boolean isUse() {
        return chatGPTProperties.isUse();
    }

    @Override
    public Long getChatAIUserId() {
        return chatGPTProperties.getAIUserId();
    }

    @Override
    protected String doChat(Message message) {
        Long uid = message.getFromUid();
        try {
            FrequencyControlDTO frequencyControlDTO = new FrequencyControlDTO();
            frequencyControlDTO.setKey(RedisKey.getKey(CHAT_FREQUENCY_PREFIX) + ":" + uid);
            frequencyControlDTO.setUnit(TimeUnit.HOURS);
            frequencyControlDTO.setCount(chatGPTProperties.getLimit());
            frequencyControlDTO.setTime(1);
            return FrequencyControlUtil.executeWithFrequencyControl(TOTAL_COUNT_WITH_IN_FIX_TIME_FREQUENCY_CONTROLLER,
                    frequencyControlDTO,
                    () -> sendRequestToGPT(message));
        } catch (FrequencyControlException e) {
            return "亲爱的,你今天找我聊了" + chatGPTProperties.getLimit() + "次了~人家累了~明天见";
        } catch (Throwable e) {
            return "系统开小差啦~~";
        }
    }

    private String sendRequestToGPT(Message message) {
        String prompt = message.getContent().replace("@" + AI_NAME, "").trim();
        Long uid = message.getFromUid();
        Long roomId = message.getRoomId();

        ChatGPTContext context = buildContext(uid, roomId, prompt);
        context = tailorContext(context);
        
        log.info("发送AI请求: uid={}, roomId={}, sessionId={}, msgCount={}", 
            uid, roomId, context.getSessionId(), context.getMsg().size());

        String text;
        try {
            saveUserMessage(context, prompt);

            Response response = ChatGPTUtils.create(chatGPTProperties.getKey())
                    .proxyUrl(chatGPTProperties.getProxyUrl())
                    .model(chatGPTProperties.getModelName())
                    .timeout(chatGPTProperties.getTimeout())
                    .maxTokens(chatGPTProperties.getMaxTokens())
                    .message(context.getMsg())
                    .send();
            text = ChatGPTUtils.parseText(response);

            context.addMsg(ChatGPTMsgBuilder.assistantMsg(text));

            saveAssistantMessage(context, text);

            cacheContext(context);
        } catch (Exception e) {
            log.warn("gpt doChat warn:", e);
            text = "我累了，明天再聊吧";
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
        ChatGPTContext context = aiContextService.buildContext(uid, roomId, 0, prompt);
        return context;
    }

    private ChatGPTContext tailorContext(ChatGPTContext context) {
        List<ChatGPTMsg> msg = context.getMsg();
        Integer totalTokens = ChatGPTUtils.countTokens(msg);
        int tokenBudget = chatGPTProperties.getMaxTokens() - 500;
        
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

    private Long userChatNumInrc(Long uid) {
        return RedisUtils.inc(RedisKey.getKey(RedisKey.USER_CHAT_NUM, uid), DateUtils.getEndTimeByToday().intValue(), TimeUnit.MILLISECONDS);
    }

    private Long getUserChatNum(Long uid) {
        Long num = RedisUtils.get(RedisKey.getKey(RedisKey.USER_CHAT_NUM, uid), Long.class);
        return num == null ? 0 : num;
    }

    @Override
    protected boolean supports(Message message) {
        if (!chatGPTProperties.isUse()) {
            return false;
        }

        MessageExtra extra = message.getExtra();
        if (extra == null) {
            return false;
        }
        if (CollectionUtils.isEmpty(extra.getAtUidList())) {
            return false;
        }
        if (!extra.getAtUidList().contains(chatGPTProperties.getAIUserId())) {
            return false;
        }

        if (StringUtils.isBlank(message.getContent())) {
            return false;
        }
        return StringUtils.contains(message.getContent(), "@" + AI_NAME)
                && StringUtils.isNotBlank(message.getContent().replace(AI_NAME, "").trim());
    }
}
