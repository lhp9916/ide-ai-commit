package com.peng.aicommit.llm;

import com.peng.aicommit.AiCommitException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OpenAiCompatibleClientTest {

    @Test
    public void appendsChatCompletionsPath() {
        assertEquals("https://api.deepseek.com/v1/chat/completions",
                OpenAiCompatibleClient.buildChatCompletionsUrl("https://api.deepseek.com/v1"));
    }

    @Test
    public void toleratesTrailingSlash() {
        assertEquals("https://api.deepseek.com/v1/chat/completions",
                OpenAiCompatibleClient.buildChatCompletionsUrl("https://api.deepseek.com/v1/"));
    }

    @Test
    public void doesNotDuplicateExistingSuffix() {
        assertEquals("https://x/chat/completions",
                OpenAiCompatibleClient.buildChatCompletionsUrl("https://x/chat/completions"));
    }

    @Test(expected = AiCommitException.class)
    public void rejectsBlankBaseUrl() {
        OpenAiCompatibleClient.buildChatCompletionsUrl("   ");
    }

    @Test
    public void requestBodyCarriesModelAndMessages() {
        String body = OpenAiCompatibleClient.buildRequestBody("deepseek-chat", "系统", "用户");
        assertTrue(body.contains("\"model\":\"deepseek-chat\""));
        assertTrue(body.contains("\"role\":\"system\""));
        assertTrue(body.contains("\"role\":\"user\""));
        assertTrue(body.contains("\"stream\":false"));
    }

    @Test
    public void endpointConfigCanBeBuiltWithoutSavedSettings() {
        OpenAiCompatibleClient.EndpointConfig config =
                new OpenAiCompatibleClient.EndpointConfig("https://example.com/v1", "my-model", 30);
        // 仅验证构造可用；实际网络调用不在此测试范围内。
        assertEquals("https://example.com/v1/chat/completions",
                OpenAiCompatibleClient.buildChatCompletionsUrl("https://example.com/v1"));
        assertTrue(config != null);
    }
}
