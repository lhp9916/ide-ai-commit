package com.peng.aicommit.core;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PromptBuilderTest {

    @Test
    public void conventionalChinesePromptMentionsChineseAndTypes() {
        String prompt = PromptBuilder.buildSystemPrompt(MessageStyle.CONVENTIONAL_ZH, "");
        assertTrue(prompt.contains("中文"));
        assertTrue(prompt.contains("Conventional Commits"));
        assertTrue(prompt.contains("feat"));
        assertTrue(prompt.contains("fix"));
    }

    @Test
    public void plainChinesePromptForbidsTypePrefix() {
        String prompt = PromptBuilder.buildSystemPrompt(MessageStyle.PLAIN_ZH, "");
        assertTrue(prompt.contains("中文"));
        assertTrue(prompt.contains("不加类型前缀"));
        assertFalse(prompt.contains("Conventional Commits"));
    }

    @Test
    public void englishPromptUsesEnglish() {
        String prompt = PromptBuilder.buildSystemPrompt(MessageStyle.CONVENTIONAL_EN, "");
        assertTrue(prompt.contains("英文"));
        assertTrue(prompt.contains("Conventional Commits"));
    }

    @Test
    public void appendsExtraPromptWhenPresent() {
        String prompt = PromptBuilder.buildSystemPrompt(MessageStyle.CONVENTIONAL_ZH, "不要使用 emoji");
        assertTrue(prompt.contains("不要使用 emoji"));
    }

    @Test
    public void ignoresBlankExtraPrompt() {
        String prompt = PromptBuilder.buildSystemPrompt(MessageStyle.CONVENTIONAL_ZH, "   ");
        assertFalse(prompt.contains("用户附加要求"));
    }

    @Test
    public void userPromptEmbedsSummary() {
        String prompt = PromptBuilder.buildUserPrompt("变更文件（1 个）：\n- a.txt (修改)");
        assertTrue(prompt.contains("a.txt"));
    }
}
