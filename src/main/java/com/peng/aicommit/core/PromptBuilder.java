package com.peng.aicommit.core;

/**
 * 组装系统提示词与用户提示词。
 */
public final class PromptBuilder {
    private PromptBuilder() {
    }

    public static String buildSystemPrompt(MessageStyle style, String extraPrompt) {
        return buildSystemPrompt(style, extraPrompt, false);
    }

    /**
     * @param strictMode 开启后要求模型严格遵守 Conventional Commits，供校验重试时使用
     */
    public static String buildSystemPrompt(MessageStyle style, String extraPrompt, boolean strictMode) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个专业的 Git 提交消息生成助手。请根据用户提供的代码变更，生成一条高质量的提交消息。\n");
        sb.append(styleRules(style, strictMode));
        sb.append("- 只输出提交消息本身，不要输出额外解释、前后缀、引号或 Markdown 代码块\n");
        if (extraPrompt != null && !extraPrompt.trim().isEmpty()) {
            sb.append("\n用户附加要求：\n").append(extraPrompt.trim()).append('\n');
        }
        return sb.toString();
    }

    private static String styleRules(MessageStyle style, boolean strictMode) {
        switch (style) {
            case PLAIN_ZH:
                return "要求：\n"
                        + "- 使用中文，单行描述这次变更做了什么\n"
                        + "- 不加类型前缀，不超过 50 字，结尾不加句号\n";
            case CONVENTIONAL_EN:
                return conventionalRules("英文", "description", strictMode,
                        "- description 使用祈使语气、小写开头，说明这次变更做了什么，"
                                + "不超过 72 字符，结尾不加句号",
                        "每条不超过 72 字符，结尾不加句号");
            case CONVENTIONAL_ZH:
            default:
                return conventionalRules("中文", "描述", strictMode,
                        "- 描述使用中文，说明这次变更做了什么，不超过 50 字，结尾不加句号",
                        "每条不超过 60 字，结尾不加句号");
        }
    }

    /**
     * 两种 Conventional 风格共用的规则骨架，避免中英两套规则逐渐跑偏。
     */
    private static String conventionalRules(String language, String descriptionLabel, boolean strictMode,
                                            String descriptionRule, String bodyLineRule) {
        StringBuilder sb = new StringBuilder();
        sb.append("要求：\n");
        sb.append("- 使用").append(language)
                .append("，严格遵循 Conventional Commits 1.0.0：`<type>(<scope>): <")
                .append(descriptionLabel).append(">`，scope 与 `!` 均为可选，不需要时省略\n");
        sb.append("- type 只能取以下之一：feat（新增功能或能力）、fix（修复缺陷）、"
                + "refactor（重构且不改变外部行为）、perf（性能优化）、"
                + "docs、style、test、build、ci、chore、revert（回退某次提交）\n");
        sb.append("- 判断要点：新增日志、埋点、开关等能力属于 feat；仅修正错误行为才用 fix；"
                + "调整措辞或格式且无功能变化用 style 或 chore\n");
        sb.append("- scope 使用简短的模块名（如 upload、auth、order），必须小写，"
                + "不要使用文件名或驼峰长名（不要写成 uploadFile 这类）；无法判断时整体省略，"
                + "不要写成空括号 ()\n");
        sb.append(descriptionRule).append('\n');
        sb.append("- 破坏性变更（删除或重命名公开 API、修改函数签名或默认值、"
                + "修改配置项或数据格式导致不兼容）必须在类型后加 `!`，"
                + "并在正文首行以 `BREAKING CHANGE: ` 开头说明影响与迁移方式；"
                + "若无法确认是否破坏兼容，则不要加 `!`\n");
        sb.append("- 如变更关联 issue，可在末尾加脚注，如 `Refs: #123` 或 `Closes: #123`\n");
        sb.append("- 在").append(descriptionLabel).append("后空一行补充正文要点，每条以 \"- \" 开头：\n");
        sb.append("  - 说明改动的目的、关键做法或影响，2 到 4 条\n");
        sb.append("  - ").append(bodyLineRule).append('\n');
        sb.append("  - 变更极其琐碎时（如仅修正错别字）可省略正文\n");
        sb.append("- 首行（含 type、scope、描述）不超过 100 字符\n");
        if (strictMode) {
            sb.append("- 本次为严格模式：输出会被机器校验，任何格式偏差都会被退回重写，务必逐条满足上述要求\n");
        }
        return sb.toString();
    }

    public static String buildUserPrompt(String changeSummary) {
        return "以下是要提交的变更，请概括出这次改动的意图，而不是逐条复述代码：\n\n" + changeSummary;
    }

    /**
     * 严格模式下校验失败时，把问题回喂给模型要求重写。
     */
    public static String buildRepairPrompt(String previousMessage, java.util.List<String> problems) {
        StringBuilder sb = new StringBuilder();
        sb.append("你上一次的输出不符合 Conventional Commits 规范，存在以下问题：\n");
        for (String problem : problems) {
            sb.append("- ").append(problem).append('\n');
        }
        sb.append("\n上一次的输出：\n").append(previousMessage).append('\n');
        sb.append("\n请重新生成，只输出修正后的提交消息，不要解释。\n");
        return sb.toString();
    }

    public static String buildConnectionTestPrompt() {
        return "这是一次连接测试。请只回复两个字：正常";
    }
}
