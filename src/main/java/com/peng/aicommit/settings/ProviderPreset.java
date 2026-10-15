package com.peng.aicommit.settings;

/**
 * 内置供应商预设。全部使用 OpenAI 兼容的 /chat/completions 协议。
 */
public enum ProviderPreset {
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat"),
    DASHSCOPE("通义千问 DashScope", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus"),
    ZHIPU("智谱 GLM", "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash"),
    MOONSHOT("Kimi Moonshot", "https://api.moonshot.cn/v1", "moonshot-v1-8k"),
    SILICONFLOW("硅基流动 SiliconFlow", "https://api.siliconflow.cn/v1", "deepseek-ai/DeepSeek-V3"),
    CUSTOM("自定义", "", "");

    private final String displayName;
    private final String defaultBaseUrl;
    private final String defaultModel;

    ProviderPreset(String displayName, String defaultBaseUrl, String defaultModel) {
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultModel = defaultModel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    /**
     * 供应商标识，同时用作密钥存储的 user name。
     */
    public String getId() {
        return name().toLowerCase();
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static ProviderPreset fromName(String name) {
        if (name != null) {
            for (ProviderPreset preset : values()) {
                if (preset.name().equalsIgnoreCase(name.trim()) || preset.getId().equalsIgnoreCase(name.trim())) {
                    return preset;
                }
            }
        }
        return DEEPSEEK;
    }
}
