package com.peng.aicommit.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MessageSanitizerTest {

    @Test
    public void stripsCodeFence() {
        String raw = "```\nfix: 修复登录超时\n```";
        assertEquals("fix: 修复登录超时", MessageSanitizer.sanitize(raw, 0));
    }

    @Test
    public void stripsFenceWithLanguage() {
        String raw = "```text\nfeat: 新增导出功能\n```";
        assertEquals("feat: 新增导出功能", MessageSanitizer.sanitize(raw, 0));
    }

    @Test
    public void stripsLabelPrefix() {
        String raw = "提交消息：\nfix: 修复空指针";
        assertEquals("fix: 修复空指针", MessageSanitizer.sanitize(raw, 0));
    }

    @Test
    public void removesSurroundingQuotesAndWhitespace() {
        String raw = "  \n \"fix: 修复问题\"  \n ";
        assertEquals("\"fix: 修复问题\"", MessageSanitizer.sanitize(raw, 0));
    }

    @Test
    public void collapsesBlankLinesAndKeepsBody() {
        String raw = "feat: 新增导出\n\n\n\n补充说明内容";
        assertEquals("feat: 新增导出\n\n补充说明内容", MessageSanitizer.sanitize(raw, 0));
    }

    @Test
    public void limitsLineCount() {
        String raw = "feat: 新增导出\n正文一\n正文二\n正文三";
        assertEquals("feat: 新增导出\n正文一", MessageSanitizer.sanitize(raw, 2));
    }

    @Test
    public void handlesNullAndBlank() {
        assertEquals("", MessageSanitizer.sanitize(null, 0));
        assertEquals("", MessageSanitizer.sanitize("   \n  \n", 0));
    }
}
