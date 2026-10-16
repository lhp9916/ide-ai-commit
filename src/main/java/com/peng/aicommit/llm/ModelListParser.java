package com.peng.aicommit.llm;

import com.peng.aicommit.AiCommitException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 解析 OpenAI 兼容的 GET /models 响应。
 * 标准格式为 {"object":"list","data":[{"id":"...","object":"model",...}]}。
 */
public final class ModelListParser {
    private ModelListParser() {
    }

    /**
     * 提取模型 id 列表，按名称排序并去重。
     *
     * @throws AiCommitException 响应结构异常或服务端返回错误
     */
    public static List<String> extractModelIds(String responseBody) {
        Object root;
        try {
            root = Json.parse(responseBody);
        } catch (RuntimeException e) {
            throw new AiCommitException("无法解析模型列表，返回内容不是合法 JSON。", e);
        }

        Set<String> ids = new LinkedHashSet<>();
        if (root instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) root;
            Object data = map.get("data");
            if (data instanceof List) {
                for (Object item : (List<?>) data) {
                    String id = idOf(item);
                    if (id != null) {
                        ids.add(id);
                    }
                }
            } else if (data instanceof Map) {
                // 少数实现返回 {"data": {...}}，此处兼容处理。
                for (Object item : ((Map<?, ?>) data).values()) {
                    String id = idOf(item);
                    if (id != null) {
                        ids.add(id);
                    }
                }
            }
        }

        if (ids.isEmpty()) {
            String error = ChatResponseParser.extractError(responseBody);
            if (error != null) {
                throw new AiCommitException("获取模型列表失败：" + error);
            }
            throw new AiCommitException("服务未返回任何模型，请检查 Base URL 与 API Key。");
        }

        List<String> result = new ArrayList<>(ids);
        result.sort(Comparator.naturalOrder());
        return result;
    }

    private static String idOf(Object item) {
        if (item instanceof String) {
            String value = ((String) item).trim();
            return value.isEmpty() ? null : value;
        }
        if (item instanceof Map) {
            Object id = ((Map<?, ?>) item).get("id");
            if (id instanceof String) {
                String value = ((String) id).trim();
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }
}
