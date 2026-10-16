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
    public void conventionalChinesePromptAsksForBodyBullets() {
        String prompt = PromptBuilder.buildSystemPrompt(MessageStyle.CONVENTIONAL_ZH, "");
        assertTrue("应要求补充正文要点", prompt.contains("正文要点"));
        assertTrue("正文要点应以 - 开头", prompt.contains("\"- \""));
    }

    @Test
    public void conventionalChinesePromptGuidesScopeNaming() {
        String prompt = PromptBuilder.buildSystemPrompt(MessageStyle.CONVENTIONAL_ZH, "");
        assertTrue("应给出 scope 命名指引", prompt.contains("scope"));
        assertTrue("应明确禁止驼峰文件名式 scope", prompt.contains("uploadFile"));
    }

    @Test
    public void conventionalChinesePromptDistinguishesFeatFromFix() {
        String prompt = PromptBuilder.buildSystemPrompt(MessageStyle.CONVENTIONAL_ZH, "");
        assertTrue("应说明新增日志等能力属于 feat", prompt.contains("日志"));
        assertTrue("应说明仅修正错误行为才用 fix", prompt.contains("修正错误行为"));
    }

    @Test
    public void englishPromptAlsoAsksForBodyBullets() {
        String prompt = PromptBuilder.buildSystemPrompt(MessageStyle.CONVENTIONAL_EN, "");
        assertTrue("英文风格同样应要求正文要点", prompt.contains("\"- \""));
    }

    @Test
    public void plainChinesePromptStillAvoidsBodyBullets() {
        String prompt = PromptBuilder.buildSystemPrompt(MessageStyle.PLAIN_ZH, "");
        assertFalse("纯描述风格不应要求正文要点", prompt.contains("正文要点"));
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
