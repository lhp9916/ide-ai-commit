package com.peng.aicommit.llm;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.HttpRequests.HttpStatusException;
import com.peng.aicommit.AiCommitException;
import com.peng.aicommit.settings.AiCommitSettings;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议的对话客户端。所有内置供应商都走 /chat/completions。
 */
public class OpenAiCompatibleClient {

    private final EndpointConfig config;

    public OpenAiCompatibleClient(AiCommitSettings settings) {
        this(EndpointConfig.fromSettings(settings));
    }

    /**
     * 供设置页「测试连接」使用：直接传入面板当前值，无需先保存。
     */
    public OpenAiCompatibleClient(EndpointConfig config) {
        this.config = config;
    }

    /**
     * 发送一次对话请求并返回助手回复的原始文本。
     *
     * @param apiKey 已解析出的密钥
     */
    public String complete(String apiKey, String systemPrompt, String userPrompt, ProgressIndicator indicator)
            throws AiCommitException {
        String url = buildChatCompletionsUrl(config.baseUrl);
        String body = buildRequestBody(config.model, systemPrompt, userPrompt);
        int timeoutMillis = config.timeoutSeconds * 1000;

        String response;
        try {
            response = HttpRequests.post(url, body)
                    .connectTimeout(timeoutMillis)
                    .readTimeout(timeoutMillis)
                    .useProxy(true)
                    .productNameAsUserAgent()
                    // 让 HTTP 错误也带上响应体，便于把服务端错误原因展示给用户。
                    .isReadResponseOnError(true)
                    .tuner(connection -> {
                        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                        connection.setRequestProperty("Accept", "application/json");
                        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                    })
                    .readString(indicator);
        } catch (HttpStatusException e) {
            throw new AiCommitException(describeHttpStatus(e), e);
        } catch (SocketTimeoutException e) {
            throw new AiCommitException("请求超时，请检查网络或在设置中增大超时时间。", e);
        } catch (UnknownHostException e) {
            throw new AiCommitException("无法解析服务地址，请检查 Base URL 与网络连接。", e);
        } catch (IOException e) {
            throw new AiCommitException("网络请求失败：" + e.getMessage(), e);
        }

        if (response == null || response.trim().isEmpty()) {
            throw new AiCommitException("服务返回了空响应。");
        }
        return ChatResponseParser.extractContent(response);
    }

    /**
     * 拼接 chat/completions 地址，兼容用户填写带或不带尾斜杠、带或不带 /v1 的 Base URL。
     */
    public static String buildChatCompletionsUrl(String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isEmpty()) {
            throw new AiCommitException("尚未配置 Base URL，请在设置中填写。");
        }
        if (trimmed.endsWith("/chat/completions")) {
            return trimmed;
        }
        return trimmed + "/chat/completions";
    }

    static String buildRequestBody(String model, String systemPrompt, String userPrompt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.3);
        payload.put("stream", false);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message("system", systemPrompt));
        messages.add(message("user", userPrompt));
        payload.put("messages", messages);
        return Json.write(payload);
    }

    private static Map<String, Object> message(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    /**
     * 一次请求所需的端点信息，便于脱离已保存设置直接测试。
     */
    public static final class EndpointConfig {
        private final String baseUrl;
        private final String model;
        private final int timeoutSeconds;

        public EndpointConfig(String baseUrl, String model, int timeoutSeconds) {
            this.baseUrl = baseUrl;
            this.model = model;
            this.timeoutSeconds = timeoutSeconds;
        }

        static EndpointConfig fromSettings(AiCommitSettings settings) {
            return new EndpointConfig(settings.getEffectiveBaseUrl(), settings.getEffectiveModel(),
                    settings.getTimeoutSeconds());
        }
    }

    /**
     * 把 HTTP 状态码翻译成对用户有意义的中文提示。
     */
    static String describeHttpStatus(HttpStatusException e) {
        String detail = ChatResponseParser.extractError(safeReadError(e));
        String suffix = detail == null ? "" : "（" + detail + "）";
        switch (e.getStatusCode()) {
            case 400:
                return "请求被拒绝，请检查模型名称是否正确。" + suffix;
            case 401:
            case 403:
                return "API Key 无效或没有权限，请在设置中检查密钥。" + suffix;
            case 404:
                return "接口地址不存在，请检查 Base URL 是否正确。" + suffix;
            case 429:
                return "请求过于频繁或额度不足，请稍后重试。" + suffix;
            default:
                if (e.getStatusCode() >= 500) {
                    return "服务端返回错误（HTTP " + e.getStatusCode() + "），请稍后重试。" + suffix;
                }
                return "请求失败（HTTP " + e.getStatusCode() + "）。" + suffix;
        }
    }

    /**
     * HttpStatusException 不携带响应体，这里尽力从异常的 URL 与状态推断可用信息。
     */
    private static String safeReadError(HttpStatusException e) {
        String message = e.getMessage();
        if (message == null) {
            return "";
        }
        int brace = message.indexOf('{');
        return brace >= 0 ? message.substring(brace) : message;
    }
}
