package com.peng.aicommit.llm;

import com.peng.aicommit.AiCommitException;

import java.util.List;
import java.util.Map;

/**
 * 解析 OpenAI 兼容的 chat/completions 响应。
 */
public final class ChatResponseParser {
    private ChatResponseParser() {
    }

    /**
     * 取出首条回复的文本内容。
     *
     * @throws AiCommitException 响应结构异常或服务端返回错误
     */
    public static String extractContent(String responseBody) {
        Object root;
        try {
            root = Json.parse(responseBody);
        } catch (RuntimeException e) {
            throw new AiCommitException("无法解析模型响应，返回内容不是合法 JSON。", e);
        }
        if (root instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) root;
            Object choices = map.get("choices");
            if (choices instanceof List && !((List<?>) choices).isEmpty()) {
                Object first = ((List<?>) choices).get(0);
                if (first instanceof Map) {
                    Map<?, ?> choice = (Map<?, ?>) first;
                    Object message = choice.get("message");
                    if (message instanceof Map) {
                        Object content = ((Map<?, ?>) message).get("content");
                        if (isNonBlankString(content)) {
                            return (String) content;
                        }
                    }
                    Object text = choice.get("text");
                    if (isNonBlankString(text)) {
                        return (String) text;
                    }
                }
            }
        }
        String error = extractError(responseBody);
        if (error != null) {
            throw new AiCommitException("模型返回错误：" + error);
        }
        throw new AiCommitException("模型响应中没有可用的提交消息内容。");
    }

    /**
     * 尝试从错误响应中取出可读的错误描述，取不到时返回 null。
     */
    public static String extractError(String responseBody) {
        try {
            Object root = Json.parse(responseBody);
            if (root instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) root;
                Object error = map.get("error");
                if (error instanceof Map) {
                    Object message = ((Map<?, ?>) error).get("message");
                    if (isNonBlankString(message)) {
                        return (String) message;
                    }
                }
                if (isNonBlankString(error)) {
                    return (String) error;
                }
                Object message = map.get("message");
                if (isNonBlankString(message)) {
                    return (String) message;
                }
            }
        } catch (RuntimeException ignored) {
            // 非 JSON 响应，交由调用方按原文处理。
        }
        return null;
    }

    private static boolean isNonBlankString(Object value) {
        return value instanceof String && !((String) value).trim().isEmpty();
    }
}
