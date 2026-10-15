package com.peng.aicommit.core;

/**
 * 单个文件的变更片段，供截断与提示词组装使用。
 */
public final class FileSection {
    private final String path;
    private final String status;
    private final boolean binary;
    private final String diffText;

    public FileSection(String path, String status, boolean binary, String diffText) {
        this.path = path;
        this.status = status;
        this.binary = binary;
        this.diffText = diffText == null ? "" : diffText;
    }

    public String getPath() {
        return path;
    }

    public String getStatus() {
        return status;
    }

    public boolean isBinary() {
        return binary;
    }

    public String getDiffText() {
        return diffText;
    }
}
