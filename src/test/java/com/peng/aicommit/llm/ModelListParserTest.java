package com.peng.aicommit.llm;

import com.peng.aicommit.AiCommitException;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ModelListParserTest {

    @Test
    public void parsesOpenAiStyleModelList() {
        String body = "{\"object\":\"list\",\"data\":["
                + "{\"id\":\"deepseek-flash\",\"object\":\"model\",\"owned_by\":\"deepseek\"},"
                + "{\"id\":\"deepseek-v4-pro\",\"object\":\"model\",\"owned_by\":\"deepseek\"}]}";
        assertEquals(Arrays.asList("deepseek-flash", "deepseek-v4-pro"),
                ModelListParser.extractModelIds(body));
    }

    @Test
    public void sortsAndDeduplicates() {
        String body = "{\"data\":[{\"id\":\"b\"},{\"id\":\"a\"},{\"id\":\"b\"}]}";
        assertEquals(Arrays.asList("a", "b"), ModelListParser.extractModelIds(body));
    }

    @Test
    public void toleratesPlainStringItems() {
        String body = "{\"data\":[\"glm-5.3\",\"glm-5.2\"]}";
        assertEquals(Arrays.asList("glm-5.2", "glm-5.3"), ModelListParser.extractModelIds(body));
    }

    @Test
    public void skipsItemsWithoutId() {
        String body = "{\"data\":[{\"id\":\"ok\"},{\"name\":\"no-id\"},{\"id\":\"  \"}]}";
        assertEquals(Arrays.asList("ok"), ModelListParser.extractModelIds(body));
    }

    @Test
    public void surfacesServerError() {
        String body = "{\"error\":{\"message\":\"Invalid Authentication\"}}";
        AiCommitException thrown = null;
        try {
            ModelListParser.extractModelIds(body);
        } catch (AiCommitException e) {
            thrown = e;
        }
        assertTrue(thrown != null && thrown.getMessage().contains("Invalid Authentication"));
    }

    @Test
    public void reportsEmptyListClearly() {
        AiCommitException thrown = null;
        try {
            ModelListParser.extractModelIds("{\"object\":\"list\",\"data\":[]}");
        } catch (AiCommitException e) {
            thrown = e;
        }
        assertTrue(thrown != null && thrown.getMessage().contains("未返回任何模型"));
    }

    @Test(expected = AiCommitException.class)
    public void rejectsNonJson() {
        ModelListParser.extractModelIds("<html>404</html>");
    }

    @Test
    public void buildsModelsUrlLikeChatCompletions() {
        assertEquals("https://api.deepseek.com/models",
                OpenAiCompatibleClient.buildModelsUrl("https://api.deepseek.com"));
        assertEquals("https://api.siliconflow.cn/v1/models",
                OpenAiCompatibleClient.buildModelsUrl("https://api.siliconflow.cn/v1/"));
        assertEquals("https://x/models",
                OpenAiCompatibleClient.buildModelsUrl("https://x/models"));
    }

    @Test
    public void extractsEveryModelFromRealShapeResponse() {
        // 取自 DeepSeek 官方 GET /models 的响应结构
        String body = "{\"object\":\"list\",\"data\":["
                + "{\"id\":\"deepseek-flash\",\"object\":\"model\",\"owned_by\":\"deepseek\","
                + "\"name\":\"DeepSeek V4.1 Flash\",\"context_window\":1000000},"
                + "{\"id\":\"deepseek-v4-pro\",\"object\":\"model\",\"owned_by\":\"deepseek\"}]}";
        List<String> ids = ModelListParser.extractModelIds(body);
        assertEquals(2, ids.size());
        assertTrue(ids.contains("deepseek-flash"));
        assertTrue(ids.contains("deepseek-v4-pro"));
    }
}
