package com.peng.aicommit.core;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PatchTruncatorTest {

    private static FileSection text(String path, String diff) {
        return new FileSection(path, "修改", false, diff);
    }

    @Test
    public void keepsAllContentWhenWithinBudget() {
        List<FileSection> sections = Arrays.asList(
                text("a.txt", "+aaa\n"),
                text("b.txt", "+bbb\n"));
        String result = PatchTruncator.render(sections, 1000, 10000);
        assertTrue(result.contains("+aaa"));
        assertTrue(result.contains("+bbb"));
        assertFalse(result.contains("已截断"));
    }

    @Test
    public void truncatesSingleOversizedFile() {
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            big.append("+line").append(i).append('\n');
        }
        String result = PatchTruncator.render(Arrays.asList(text("big.txt", big.toString())), 50, 10000);
        assertTrue(result.contains("该文件差异已截断"));
        assertTrue(result.length() < big.length());
    }

    @Test
    public void omitsFilesBeyondTotalBudgetButKeepsFileList() {
        List<FileSection> sections = new ArrayList<>();
        sections.add(text("a.txt", repeat("+a\n", 50)));
        sections.add(text("b.txt", repeat("+b\n", 50)));
        sections.add(text("c.txt", repeat("+c\n", 50)));
        String result = PatchTruncator.render(sections, 10000, 120);

        // 文件清单始终完整
        assertTrue(result.contains("- a.txt"));
        assertTrue(result.contains("- b.txt"));
        assertTrue(result.contains("- c.txt"));
        assertTrue(result.contains("因超出长度上限被省略"));
    }

    @Test
    public void skipsBinaryContentButListsFile() {
        List<FileSection> sections = Arrays.asList(
                new FileSection("logo.png", "新增", true, "binary-data"));
        String result = PatchTruncator.render(sections, 1000, 10000);
        assertTrue(result.contains("- logo.png"));
        assertFalse(result.contains("binary-data"));
    }

    @Test
    public void reportsFileCount() {
        String result = PatchTruncator.render(Arrays.asList(text("a.txt", "+a\n")), 1000, 10000);
        assertTrue(result.contains("1 个"));
    }

    private static String repeat(String value, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(value);
        }
        return sb.toString();
    }
}
