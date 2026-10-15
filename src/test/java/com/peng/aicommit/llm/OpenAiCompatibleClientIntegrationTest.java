package com.peng.aicommit.llm;

import com.peng.aicommit.AiCommitException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 用本地 HTTP 服务端验证真实请求行为。
 * 这里能拦住「请求体没发出去」这类只在运行时暴露的问题：
 * HttpRequests.post(url, x) 的第二个参数是 Content-Type，不是请求体。
 */
public class OpenAiCompatibleClientIntegrationTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastAuth = new AtomicReference<>();
    private final AtomicReference<String> lastContentType = new AtomicReference<>();
    private volatile int responseCode = 200;
    private volatile String responseBody = "{\"choices\":[{\"message\":{\"content\":\"正常\"}}]}";

    @Before
    public void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", this::handle);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    @After
    public void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handle(HttpExchange exchange) throws java.io.IOException {
        lastContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
        lastAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
        lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

        byte[] payload = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(responseCode, payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }

    private OpenAiCompatibleClient client() {
        return new OpenAiCompatibleClient(
                new OpenAiCompatibleClient.EndpointConfig(baseUrl, "test-model", 10));
    }

    @Test
    public void sendsRequestBodyWithModelAndMessages() {
        String reply = client().complete("secret-key", "系统提示", "用户提示", null);

        assertEquals("正常", reply);
        String body = lastBody.get();
        assertNotNull("请求体不应为空", body);
        assertTrue("请求体必须包含 model", body.contains("\"model\":\"test-model\""));
        assertTrue("请求体必须包含 system 消息", body.contains("系统提示"));
        assertTrue("请求体必须包含 user 消息", body.contains("用户提示"));
    }

    @Test
    public void sendsJsonContentTypeAndAuthorization() {
        client().complete("secret-key", "s", "u", null);

        assertTrue("Content-Type 必须是 application/json，而不是请求体本身",
                lastContentType.get() != null && lastContentType.get().startsWith("application/json"));
        assertEquals("Bearer secret-key", lastAuth.get());
    }

    @Test
    public void surfacesServerErrorMessageOnBadRequest() {
        responseCode = 400;
        responseBody = "{\"error\":{\"message\":\"Model Not Exist\",\"type\":\"invalid_request_error\"}}";

        try {
            client().complete("k", "s", "u", null);
            fail("应当抛出 AiCommitException");
        } catch (AiCommitException e) {
            assertTrue("提示应包含服务端原因，实际：" + e.getMessage(),
                    e.getMessage().contains("Model Not Exist"));
            assertTrue("提示应为中文可读文案，实际：" + e.getMessage(),
                    e.getMessage().contains("请求被拒绝"));
        }
    }

    @Test
    public void reportsInvalidApiKey() {
        responseCode = 401;
        responseBody = "{\"error\":{\"message\":\"Authentication Fails\"}}";

        try {
            client().complete("bad", "s", "u", null);
            fail("应当抛出 AiCommitException");
        } catch (AiCommitException e) {
            assertTrue("提示应指出密钥问题，实际：" + e.getMessage(),
                    e.getMessage().contains("API Key"));
        }
    }

    @Test
    public void handlesNonJsonErrorBody() {
        responseCode = 502;
        responseBody = "<html><body>Bad Gateway</body></html>";

        try {
            client().complete("k", "s", "u", null);
            fail("应当抛出 AiCommitException");
        } catch (AiCommitException e) {
            assertTrue("非 JSON 错误体也应给出提示，实际：" + e.getMessage(),
                    e.getMessage().contains("502"));
        }
    }
}
