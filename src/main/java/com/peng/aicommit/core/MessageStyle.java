package com.peng.aicommit.core;

/**
 * 生成提交消息的风格。
 */
public enum MessageStyle {
    CONVENTIONAL_ZH("中文 Conventional Commits"),
    PLAIN_ZH("中文纯描述"),
    CONVENTIONAL_EN("英文 Conventional Commits");

    private final String displayName;

    MessageStyle(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static MessageStyle fromName(String name) {
        if (name != null) {
            for (MessageStyle style : values()) {
                if (style.name().equalsIgnoreCase(name.trim())) {
                    return style;
                }
            }
        }
        return CONVENTIONAL_ZH;
    }
}
