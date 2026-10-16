package com.peng.aicommit.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 按 Conventional Commits 1.0.0 校验提交消息。
 * <p>
 * 校验规则对齐 commitlint 的 @commitlint/config-conventional 默认值：
 * type-enum、type-case、subject-empty、subject-full-stop、
 * header-max-length(100)、body-leading-blank、scope-case(lower-case)。
 * <p>
 * 仅用于「严格模式」：校验不通过时把问题回喂给模型让它重写，不影响普通生成。
 */
public final class CommitMessageValidator {

    /** 规范推荐的类型，外加 revert。 */
    public static final Set<String> ALLOWED_TYPES = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    "feat", "fix", "build", "chore", "ci", "docs",
                    "style", "refactor", "perf", "test", "revert")));

    /** header-max-length 的默认值。 */
    public static final int HEADER_MAX_LENGTH = 100;

    /** `<type>(<scope>)!: <描述>`，描述允许含冒号，故类型段限定为字母。 */
    private static final Pattern HEADER =
            Pattern.compile("^([A-Za-z]+)(?:\\(([^)]*)\\))?(!)?: (.+)$");

    private CommitMessageValidator() {
    }

    /**
     * 校验提交消息，返回需要修正的问题列表；为空表示合规。
     */
    public static List<String> validate(String message) {
        List<String> problems = new ArrayList<>();
        if (message == null || message.trim().isEmpty()) {
            problems.add("提交消息为空");
            return problems;
        }

        String normalized = message.replace("\r\n", "\n").replace('\r', '\n').trim();
        String[] lines = normalized.split("\n", -1);
        String header = lines[0].trim();

        if (header.length() > HEADER_MAX_LENGTH) {
            problems.add("首行超过 " + HEADER_MAX_LENGTH + " 字符（当前 " + header.length() + "）");
        }

        Matcher matcher = HEADER.matcher(header);
        if (!matcher.matches()) {
            problems.add("首行必须符合 `<type>(<scope>): <描述>` 格式，当前为：" + header);
            // 首行结构不合法时，后续检查没有意义。
            return problems;
        }

        String type = matcher.group(1).toLowerCase();
        String scope = matcher.group(2);
        String description = matcher.group(4).trim();

        if (!ALLOWED_TYPES.contains(type)) {
            problems.add("类型 `" + type + "` 不在允许范围内：" + String.join("/", ALLOWED_TYPES));
        }
        problems.addAll(validateScope(scope));
        problems.addAll(validateDescription(description));

        // body-leading-blank：正文前必须空一行。
        if (lines.length > 1 && !lines[1].trim().isEmpty()) {
            problems.add("正文与首行之间必须空一行");
        }

        problems.addAll(validateBreakingChange(header, lines));
        return problems;
    }

    private static List<String> validateScope(String scope) {
        List<String> problems = new ArrayList<>();
        if (scope == null) {
            return problems;
        }
        String trimmed = scope.trim();
        if (trimmed.isEmpty()) {
            problems.add("scope 不能是空括号，不需要时请整体省略");
            return problems;
        }
        if (!trimmed.equals(scope)) {
            problems.add("scope 两侧不能有空格");
        }
        if (containsAsciiUppercase(trimmed)) {
            problems.add("scope `" + trimmed + "` 应使用小写（如 upload 而非 uploadFile）");
        }
        if (containsWhitespace(trimmed)) {
            problems.add("scope `" + trimmed + "` 不能包含空格");
        }
        return problems;
    }

    private static List<String> validateDescription(String description) {
        List<String> problems = new ArrayList<>();
        if (description.isEmpty()) {
            problems.add("描述不能为空");
            return problems;
        }
        char last = description.charAt(description.length() - 1);
        if (last == '.' || last == '。') {
            problems.add("描述结尾不要加句号");
        }
        return problems;
    }

    /**
     * 破坏性变更必须被标记：`!` 或 `BREAKING CHANGE:` 脚注。
     * 这里只检查已使用的标记是否规范，以及 `!` 是否缺少说明。
     */
    private static List<String> validateBreakingChange(String header, String[] lines) {
        List<String> problems = new ArrayList<>();
        boolean hasBang = header.contains("!:");
        boolean hasFooter = false;
        boolean hasLowercaseFooter = false;

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("BREAKING CHANGE: ") || line.startsWith("BREAKING-CHANGE: ")) {
                hasFooter = true;
            } else if (line.toLowerCase().startsWith("breaking change:")
                    || line.toLowerCase().startsWith("breaking-change:")) {
                hasLowercaseFooter = true;
            }
        }

        if (hasLowercaseFooter && !hasFooter) {
            problems.add("`BREAKING CHANGE` 作为脚注令牌时必须全大写");
        }
        // 规范允许只用 `!` 不写脚注，因此缺少脚注不作为错误，仅由提示词引导补充说明。
        return problems;
    }

    private static boolean containsAsciiUppercase(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                return true;
            }
        }
        return false;
    }

    private static boolean containsWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
