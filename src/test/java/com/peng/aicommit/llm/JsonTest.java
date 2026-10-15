package com.peng.aicommit.llm;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JsonTest {

    @Test
    public void writesNestedStructure() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", "deepseek-chat");
        payload.put("stream", false);
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", "第一行\n第二行 \"引号\"");
        messages.add(message);
        payload.put("messages", messages);

        String json = Json.write(payload);
        assertTrue(json.contains("\"model\":\"deepseek-chat\""));
        assertTrue(json.contains("\"stream\":false"));
        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\\"引号\\\""));
    }

    @Test
    public void roundTripsUnicodeAndEscapes() {
        String original = "修复 登录/超时\t路径 C:\\tmp \"引号\" \u0001";
        String json = Json.write(original);
        Object parsed = Json.parse(json);
        assertEquals(original, parsed);
    }

    @Test
    public void parsesNestedResponse() {
        String body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"fix: 修复登录超时\"}}],"
                + "\"usage\":{\"total_tokens\":42}}";
        Object root = Json.parse(body);
        assertTrue(root instanceof Map);
        Map<?, ?> map = (Map<?, ?>) root;
        List<?> choices = (List<?>) map.get("choices");
        Map<?, ?> first = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) first.get("message");
        assertEquals("fix: 修复登录超时", message.get("content"));
        Map<?, ?> usage = (Map<?, ?>) map.get("usage");
        assertEquals(42.0, (Double) usage.get("total_tokens"), 0.0001);
    }

    @Test
    public void parsesEmptyContainersAndNulls() {
        assertEquals(0, ((List<?>) Json.parse("[]")).size());
        assertEquals(0, ((Map<?, ?>) Json.parse("{}")).size());
        assertNull(Json.parse("null"));
        assertEquals(Boolean.TRUE, Json.parse("true"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTrailingGarbage() {
        Json.parse("{\"a\":1} trailing");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnterminatedString() {
        Json.parse("\"abc");
    }
}
