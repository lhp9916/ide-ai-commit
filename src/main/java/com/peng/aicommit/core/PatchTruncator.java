package com.peng.aicommit.core;

import java.util.List;

/**
 * 把变更片段渲染成适合放进提示词的文本，并在超出预算时按文件截断。
 * 无论是否截断，文件列表始终完整保留。
 */
public final class PatchTruncator {
    private PatchTruncator() {
    }

    public static String render(List<FileSection> sections, int perFileChars, int totalChars) {
        StringBuilder sb = new StringBuilder();
        sb.append("变更文件（").append(sections.size()).append(" 个）：\n");
        for (FileSection section : sections) {
            sb.append("- ").append(section.getPath()).append(" (").append(section.getStatus()).append(")\n");
        }

        int effectivePerFile = perFileChars > 0 ? perFileChars : Integer.MAX_VALUE;
        int effectiveTotal = totalChars > 0 ? totalChars : Integer.MAX_VALUE;
        int used = 0;
        int omitted = 0;

        sb.append("\n差异内容：\n");
        for (FileSection section : sections) {
            if (section.isBinary()) {
                continue;
            }
            if (used >= effectiveTotal) {
                omitted++;
                continue;
            }
            String text = section.getDiffText();
            int remaining = effectiveTotal - used;
            int budget = Math.min(effectivePerFile, remaining);
            boolean truncated = text.length() > budget;
            String chunk = truncated ? text.substring(0, budget) : text;
            sb.append(chunk);
            if (!chunk.endsWith("\n")) {
                sb.append('\n');
            }
            if (truncated) {
                sb.append("…（该文件差异已截断）\n");
            }
            used += chunk.length();
        }
        if (omitted > 0) {
            sb.append("\n…（另有 ").append(omitted).append(" 个文件的差异因超出长度上限被省略）\n");
        }
        return sb.toString();
    }
}
