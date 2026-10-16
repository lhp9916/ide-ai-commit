package com.peng.aicommit.core;

/**
 * 组装系统提示词与用户提示词。
 */
public final class PromptBuilder {
    private PromptBuilder() {
    }

    public static String buildSystemPrompt(MessageStyle style, String extraPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个专业的 Git 提交消息生成助手。请根据用户提供的代码变更，生成一条高质量的提交消息。\n");
        sb.append(styleRules(style));
        sb.append("- 只输出提交消息本身，不要输出额外解释、前后缀、引号或 Markdown 代码块\n");
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
                        + "- 使用英文，遵循 Conventional Commits：`<type>(<scope>): <description>`\n"
                        + "- type 取值与判断：feat（新增功能或能力）、fix（修复缺陷）、"
                        + "refactor（重构且不改变外部行为）、perf（性能优化）、docs、style、test、build、ci、chore\n"
                        + "- scope 使用简短的模块名（如 upload、auth、order），不要使用文件名或驼峰长名；"
                        + "无法判断时省略\n"
                        + "- description 使用祈使语气、小写开头，说明这次变更做了什么，不超过 72 字符，结尾不加句号\n"
                        + "- 在 description 后空一行补充正文要点，每条以 \"- \" 开头：\n"
                        + "  - 说明改动的目的、关键做法或影响，2 到 4 条\n"
                        + "  - 每条不超过 72 字符，结尾不加句号\n"
                        + "  - 变更极其琐碎时（如仅修正错别字）可省略正文\n";
            case CONVENTIONAL_ZH:
            default:
                return "要求：\n"
                        + "- 使用中文，遵循 Conventional Commits：`<type>(<scope>): <描述>`\n"
                        + "- type 取值与判断：feat（新增功能或能力）、fix（修复缺陷）、"
                        + "refactor（重构且不改变外部行为）、perf（性能优化）、docs、style、test、build、ci、chore\n"
                        + "- 判断要点：新增日志、埋点、开关等能力属于 feat；仅修正错误行为才用 fix；"
                        + "调整措辞或格式且无功能变化用 style 或 chore\n"
                        + "- scope 使用简短的模块名（如 upload、auth、order），不要使用文件名或驼峰长名"
                        + "（不要写成 uploadFile 这类）；无法判断时省略\n"
                        + "- 描述使用中文，说明这次变更做了什么，不超过 50 字，结尾不加句号\n"
                        + "- 在描述后空一行补充正文要点，每条以 \"- \" 开头：\n"
                        + "  - 说明改动的目的、关键做法或影响，2 到 4 条\n"
                        + "  - 每条不超过 60 字，结尾不加句号\n"
                        + "  - 变更极其琐碎时（如仅修正错别字）可省略正文\n";
        }
    }

    public static String buildUserPrompt(String changeSummary) {
        return "以下是要提交的变更，请概括出这次改动的意图，而不是逐条复述代码：\n\n" + changeSummary;
    }

    public static String buildConnectionTestPrompt() {
        return "这是一次连接测试。请只回复两个字：正常";
    }
}
