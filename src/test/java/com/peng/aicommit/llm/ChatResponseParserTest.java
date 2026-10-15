package com.peng.aicommit.llm;

import com.peng.aicommit.AiCommitException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ChatResponseParserTest {

    @Test
    public void extractsMessageContent() {
        String body = "{\"choices\":[{\"message\":{\"content\":\"fix: 修复登录超时\"}}]}";
        assertEquals("fix: 修复登录超时", ChatResponseParser.extractContent(body));
    }

    @Test
    public void fallsBackToCompletionText() {
        String body = "{\"choices\":[{\"text\":\"chore: 更新依赖\"}]}";
        assertEquals("chore: 更新依赖", ChatResponseParser.extractContent(body));
    }

    @Test
    public void surfacesServerErrorMessage() {
        String body = "{\"error\":{\"message\":\"Incorrect API key provided\"}}";
        AiCommitException thrown = null;
        try {
            ChatResponseParser.extractContent(body);
        } catch (AiCommitException e) {
            thrown = e;
        }
        assertEquals(true, thrown != null && thrown.getMessage().contains("Incorrect API key provided"));
    }

    @Test
    public void reportsMissingChoices() {
        AiCommitException thrown = null;
        try {
            ChatResponseParser.extractContent("{\"id\":\"x\"}");
        } catch (AiCommitException e) {
            thrown = e;
        }
        assertEquals(true, thrown != null && thrown.getMessage().contains("没有可用"));
    }

    @Test(expected = AiCommitException.class)
    public void rejectsNonJson() {
        ChatResponseParser.extractContent("<html>502 Bad Gateway</html>");
    }

    @Test
    public void extractErrorReturnsNullForPlainText() {
        assertNull(ChatResponseParser.extractError("Bad Gateway"));
    }
}
