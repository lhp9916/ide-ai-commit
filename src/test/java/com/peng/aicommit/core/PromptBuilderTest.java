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

    @Test
    public void conventionalChinesePromptGuidesBreakingChange() {
        String prompt = PromptBuilder.buildSystemPrompt(MessageStyle.CONVENTIONAL_ZH, "");
        assertTrue("应说明破坏性变更的标记方式", prompt.contains("破坏性变更"));
        assertTrue("应要求使用 ! 标记", prompt.contains("`!`"));
        assertTrue("应要求 BREAKING CHANGE 脚注", prompt.contains("BREAKING CHANGE"));
    }

    @Test
    public void conventionalChinesePromptSupportsRevertAndFooters() {
        String prompt = PromptBuilder.buildSystemPrompt(MessageStyle.CONVENTIONAL_ZH, "");
        assertTrue("类型列表应包含 revert", prompt.contains("revert"));
        assertTrue("应说明 issue 脚注写法", prompt.contains("Refs") || prompt.contains("Closes"));
    }

    @Test
    public void conventionalChinesePromptStatesHeaderLimit() {
        String prompt = PromptBuilder.buildSystemPrompt(MessageStyle.CONVENTIONAL_ZH, "");
        assertTrue("应给出首行长度上限以对齐 header-max-length", prompt.contains("100"));
    }

    @Test
    public void strictModePromptIsMarked() {
        String normal = PromptBuilder.buildSystemPrompt(MessageStyle.CONVENTIONAL_ZH, "", false);
        String strict = PromptBuilder.buildSystemPrompt(MessageStyle.CONVENTIONAL_ZH, "", true);
        assertFalse("普通模式不应出现严格模式字样", normal.contains("严格模式"));
        assertTrue("严格模式应明确标注", strict.contains("严格模式"));
    }

    @Test
    public void legacyTwoArgBuildStillWorks() {
        String prompt = PromptBuilder.buildSystemPrompt(MessageStyle.CONVENTIONAL_ZH, "");
        assertFalse("二参重载默认应为非严格模式", prompt.contains("严格模式"));
    }

    @Test
    public void repairPromptCarriesProblemsAndPreviousOutput() {
        String prompt = PromptBuilder.buildRepairPrompt(
                "fix(uploadFile): 修复上传。",
                java.util.Arrays.asList("描述结尾不要加句号", "scope 应使用小写"));
        assertTrue("应包含上一次输出以便模型对照", prompt.contains("fix(uploadFile): 修复上传。"));
        assertTrue("应包含第一条问题", prompt.contains("描述结尾不要加句号"));
        assertTrue("应包含第二条问题", prompt.contains("scope 应使用小写"));
        assertTrue("应要求只输出提交消息", prompt.contains("只输出"));
    }
}
