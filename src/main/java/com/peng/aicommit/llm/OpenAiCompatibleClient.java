package com.peng.aicommit.llm;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.HttpRequests.HttpStatusException;
import com.peng.aicommit.AiCommitException;
import com.peng.aicommit.settings.AiCommitSettings;

import java.io.IOException;
import java.net.HttpURLConnection;
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
            // 注意：HttpRequests.post(url, contentType) 的第二个参数是 Content-Type 而非请求体，
            // 请求体必须在 connect 回调里通过 request.write(...) 发送。
            response = HttpRequests.post(url, "application/json")
                    .connectTimeout(timeoutMillis)
                    .readTimeout(timeoutMillis)
                    .useProxy(true)
                    .productNameAsUserAgent()
                    // 自行处理错误状态码，以便读取响应体并给出中文提示。
                    .throwStatusCodeException(false)
                    .tuner(connection -> {
                        connection.setRequestProperty("Accept", "application/json");
                        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                    })
                    .connect(request -> {
                        request.write(body);
                        int statusCode = ((HttpURLConnection) request.getConnection()).getResponseCode();
                        if (statusCode >= 400) {
                            throw new HttpStatusError(statusCode, request.readError());
                        }
                        return request.readString(indicator);
                    });
        } catch (HttpStatusError e) {
            throw new AiCommitException(describeHttpStatus(e.getStatusCode(), e.getBody()), e);
        } catch (HttpStatusException e) {
            throw new AiCommitException(describeHttpStatus(e.getStatusCode(), safeReadError(e)), e);
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
        return appendPath(baseUrl, "/chat/completions");
    }

    /**
     * 拼接 GET /models 地址，用于拉取该供应商当前可用的模型列表。
     */
    public static String buildModelsUrl(String baseUrl) {
        return appendPath(baseUrl, "/models");
    }

    private static String appendPath(String baseUrl, String suffix) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isEmpty()) {
            throw new AiCommitException("尚未配置 Base URL，请在设置中填写。");
        }
        if (trimmed.endsWith(suffix)) {
            return trimmed;
        }
        return trimmed + suffix;
    }

    /**
     * 拉取该供应商当前可用的模型列表。模型更新频繁，由用户按需刷新而不是写死在插件里。
     */
    public List<String> listModels(String apiKey, ProgressIndicator indicator) throws AiCommitException {
        String url = buildModelsUrl(config.baseUrl);
        int timeoutMillis = config.timeoutSeconds * 1000;

        String response;
        try {
            response = HttpRequests.request(url)
                    .connectTimeout(timeoutMillis)
                    .readTimeout(timeoutMillis)
                    .useProxy(true)
                    .productNameAsUserAgent()
                    .throwStatusCodeException(false)
                    .tuner(connection -> {
                        connection.setRequestProperty("Accept", "application/json");
                        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                    })
                    .connect(request -> {
                        int statusCode = ((HttpURLConnection) request.getConnection()).getResponseCode();
                        if (statusCode >= 400) {
                            throw new HttpStatusError(statusCode, request.readError());
                        }
                        return request.readString(indicator);
                    });
        } catch (HttpStatusError e) {
            throw new AiCommitException(describeHttpStatus(e.getStatusCode(), e.getBody()), e);
        } catch (HttpStatusException e) {
            throw new AiCommitException(describeHttpStatus(e.getStatusCode(), safeReadError(e)), e);
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
        return ModelListParser.extractModelIds(response);
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
     * 非 2xx 响应。携带状态码与响应体，便于翻译成用户可读的提示。
     */
    static class HttpStatusError extends RuntimeException {
        private final int statusCode;
        private final String body;

        HttpStatusError(int statusCode, String body) {
            super("HTTP " + statusCode);
            this.statusCode = statusCode;
            this.body = body;
        }

        int getStatusCode() {
            return statusCode;
        }

        String getBody() {
            return body;
        }
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
    static String describeHttpStatus(int statusCode, String responseBody) {
        String detail = ChatResponseParser.extractError(responseBody);
        if (detail == null && responseBody != null && !responseBody.trim().isEmpty()) {
            String trimmed = responseBody.trim();
            // 非 JSON 的响应体（如网关 HTML）截断后直接展示，避免提示为空。
            detail = trimmed.length() > 200 ? trimmed.substring(0, 200) + "…" : trimmed;
        }
        String suffix = detail == null ? "" : "（" + detail + "）";
        switch (statusCode) {
            case 400:
                return "请求被拒绝，请检查模型名称与 Base URL 是否正确。" + suffix;
            case 401:
            case 403:
                return "API Key 无效或没有权限，请在设置中检查密钥。" + suffix;
            case 404:
                return "接口地址不存在，请检查 Base URL 是否正确。" + suffix;
            case 429:
                return "请求过于频繁或额度不足，请稍后重试。" + suffix;
            default:
                if (statusCode >= 500) {
                    return "服务端返回错误（HTTP " + statusCode + "），请稍后重试。" + suffix;
                }
                return "请求失败（HTTP " + statusCode + "）。" + suffix;
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
