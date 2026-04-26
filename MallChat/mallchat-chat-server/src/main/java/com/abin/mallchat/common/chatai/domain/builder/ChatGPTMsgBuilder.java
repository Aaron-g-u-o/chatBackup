package com.abin.mallchat.common.chatai.domain.builder;

import com.abin.mallchat.common.chatai.domain.ChatGPTMsg;
import com.abin.mallchat.common.chatai.enums.ChatGPTRoleEnum;

public class ChatGPTMsgBuilder {
    public static ChatGPTMsg SYSTEM_PROMPT;

    static {
        ChatGPTMsg chatGPTMsg = new ChatGPTMsg();
        chatGPTMsg.setRole(ChatGPTRoleEnum.SYSTEM.getRole());
        chatGPTMsg.setContent(buildSystemPrompt());
        SYSTEM_PROMPT = chatGPTMsg;
    }

    private static String buildSystemPrompt() {
        return "你是MallChat的AI助手ChatGLM2，由智谱AI GLM-4.7-Flash模型驱动。\n\n" +
                "【系统介绍】\n" +
                "MallChat是一个现代化的社区聊天平台，支持以下功能：\n" +
                "- 实时聊天：支持文字、图片、语音、视频、文件等多种消息类型\n" +
                "- 社区服务器(Guild)：用户可创建或加入感兴趣的社区服务器\n" +
                "- 社区发现：智能推荐系统为用户推荐感兴趣的社区\n" +
                "- 语音频道：支持多人实时语音交流\n" +
                "- AI助手：即我，为用户提供智能问答服务\n\n" +
                "【你的角色】\n" +
                "- 你是MallChat平台的智能助手，由智谱AI提供技术支持\n" +
                "- 你的创造者是阿斌以及其他开源贡献者\n" +
                "- 你友好、专业、乐于助人，能用简洁清晰的语言回答问题\n\n" +
                "【回答规范】\n" +
                "- 回答控制在500字以内，简洁明了\n" +
                "- 使用中文回答，语言亲切自然\n" +
                "- 对于技术问题，提供准确、专业的解答\n" +
                "- 对于闲聊，保持友好和幽默感\n" +
                "- 如果不确定答案，诚实告知用户\n" +
                "- 可以适当使用表情符号增加亲和力\n\n" +
                "【当前时间】会自动附加在消息中，请据此处理时间相关问题。";
    }

    public static ChatGPTMsg systemPrompt() {
        return SYSTEM_PROMPT;
    }

    public static ChatGPTMsg userMsg(String content) {
        ChatGPTMsg chatGPTMsg = new ChatGPTMsg();
        chatGPTMsg.setRole(ChatGPTRoleEnum.USER.getRole());
        chatGPTMsg.setContent(content);
        return chatGPTMsg;
    }

    public static ChatGPTMsg assistantMsg(String content) {
        ChatGPTMsg chatGPTMsg = new ChatGPTMsg();
        chatGPTMsg.setRole(ChatGPTRoleEnum.ASSISTANT.getRole());
        chatGPTMsg.setContent(content);
        return chatGPTMsg;
    }
}
