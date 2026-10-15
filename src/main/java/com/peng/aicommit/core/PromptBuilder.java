package com.peng.aicommit.core;

/**
 * 组装系统提示词与用户提示词。
 */
public final class PromptBuilder {
    private PromptBuilder() {
    }

    public static String buildSystemPrompt(MessageStyle style, String extraPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个专业的 Git 提交消息生成助手。请根据用户提供的代码变更，生成一条提交消息。\n");
        sb.append(styleRules(style));
        sb.append("- 只输出提交消息本身，不要输出解释、前后缀、引号或 Markdown 代码块\n");
        if (extraPrompt != null && !extraPrompt.trim().isEmpty()) {
            sb.append("\n用户附加要求：\n").append(extraPrompt.trim()).append('\n');
        }
        return sb.toString();
    }

    private static String styleRules(MessageStyle style) {
        switch (style) {
            case PLAIN_ZH:
                return "要求：\n"
                        + "- 使用中文，单行描述这次变更做了什么\n"
                        + "- 不加类型前缀，不超过 50 字，结尾不加句号\n";
            case CONVENTIONAL_EN:
                return "要求：\n"
                        + "- 使用英文，遵循 Conventional Commits：`<type>(<scope>): <description>`，scope 可省略\n"
                        + "- type 取 feat/fix/docs/style/refactor/perf/test/build/ci/chore/revert\n"
                        + "- description 使用祈使语气、小写开头，不超过 72 字符，结尾不加句号\n"
                        + "- 如需补充说明，空一行后写正文，每行不超过 72 字符\n";
            case CONVENTIONAL_ZH:
            default:
                return "要求：\n"
                        + "- 使用中文，遵循 Conventional Commits：`<type>(<scope>): <描述>`，scope 可省略\n"
                        + "- type 取 feat/fix/docs/style/refactor/perf/test/build/ci/chore/revert\n"
                        + "- 描述使用中文，简洁准确，不超过 50 字，结尾不加句号\n"
                        + "- 如需补充说明，空一行后写正文，每行不超过 72 字\n";
        }
    }

    public static String buildUserPrompt(String changeSummary) {
        return "以下是要提交的变更：\n\n" + changeSummary;
    }

    public static String buildConnectionTestPrompt() {
        return "这是一次连接测试。请只回复两个字：正常";
    }
}
