package com.peng.aicommit.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 清理模型输出：剥离 Markdown 代码围栏、提示性前缀、多余空行。
 */
public final class MessageSanitizer {
    private MessageSanitizer() {
    }

    public static String sanitize(String raw, int maxLines) {
        if (raw == null) {
            return "";
        }
        String text = raw.replace("\r\n", "\n").replace('\r', '\n').trim();
        text = stripCodeFence(text);

        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n", -1)) {
            String trimmed = line.trim();
            if (isLabelLine(trimmed)) {
                continue;
            }
            lines.add(line.replaceAll("[ \t]+$", ""));
        }
        lines = collapseBlankLines(lines);
        while (!lines.isEmpty() && lines.get(0).trim().isEmpty()) {
            lines.remove(0);
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).trim().isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        if (maxLines > 0 && lines.size() > maxLines) {
            lines = new ArrayList<>(lines.subList(0, maxLines));
        }
        return String.join("\n", lines).trim();
    }

    private static String stripCodeFence(String text) {
        if (!text.startsWith("```")) {
            return text;
        }
        int firstBreak = text.indexOf('\n');
        if (firstBreak < 0) {
            // 形如 ```text``` 的单行围栏
            return text.replace("`", "").trim();
        }
        String body = text.substring(firstBreak + 1);
        int lastFence = body.lastIndexOf("```");
        if (lastFence >= 0) {
            body = body.substring(0, lastFence);
        }
        return body.trim();
    }

    private static boolean isLabelLine(String trimmedLine) {
        String lower = trimmedLine.toLowerCase();
        return lower.equals("提交消息：") || lower.equals("提交消息:")
                || lower.equals("提交信息：") || lower.equals("提交信息:")
                || lower.equals("commit message:") || lower.equals("commit message：")
                || lower.equals("commitmessage:") || lower.equals("message:");
    }

    private static List<String> collapseBlankLines(List<String> lines) {
        List<String> result = new ArrayList<>();
        boolean previousBlank = false;
        for (String line : lines) {
            boolean blank = line.trim().isEmpty();
            if (blank && previousBlank) {
                continue;
            }
            result.add(line);
            previousBlank = blank;
        }
        return result;
    }
}
