package com.peng.aicommit.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.peng.aicommit.core.MessageStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 应用级设置。API Key 不在此处持久化，见 {@link ApiKeyStore}。
 */
@State(name = "AiCommitSettings", storages = @Storage("aiCommit.xml"))
public class AiCommitSettings implements PersistentStateComponent<AiCommitSettings.State> {

    public static final int DEFAULT_PER_FILE_CHARS = 6000;
    public static final int DEFAULT_TOTAL_CHARS = 24000;
    public static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private State state = new State();

    public static AiCommitSettings getInstance() {
        return ApplicationManager.getApplication().getService(AiCommitSettings.class);
    }

    @Override
    public @NotNull State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public ProviderPreset getProvider() {
        return ProviderPreset.fromName(state.provider);
    }

    public void setProvider(ProviderPreset provider) {
        state.provider = provider.name();
    }

    /**
     * 返回生效的 Base URL：优先用户填写值，其次预设默认值。
     */
    public String getEffectiveBaseUrl() {
        String value = state.baseUrl == null ? "" : state.baseUrl.trim();
        return value.isEmpty() ? getProvider().getDefaultBaseUrl() : value;
    }

    public String getRawBaseUrl() {
        return state.baseUrl == null ? "" : state.baseUrl;
    }

    public void setRawBaseUrl(String baseUrl) {
        state.baseUrl = baseUrl;
    }

    public String getEffectiveModel() {
        String value = state.model == null ? "" : state.model.trim();
        return value.isEmpty() ? getProvider().getDefaultModel() : value;
    }

    public String getRawModel() {
        return state.model == null ? "" : state.model;
    }

    public void setRawModel(String model) {
        state.model = model;
    }

    public MessageStyle getMessageStyle() {
        return MessageStyle.fromName(state.messageStyle);
    }

    public void setMessageStyle(MessageStyle style) {
        state.messageStyle = style.name();
    }

    public String getExtraPrompt() {
        return state.extraPrompt == null ? "" : state.extraPrompt;
    }

    public void setExtraPrompt(String extraPrompt) {
        state.extraPrompt = extraPrompt;
    }

    public int getPerFileChars() {
        return state.perFileChars > 0 ? state.perFileChars : DEFAULT_PER_FILE_CHARS;
    }

    public void setPerFileChars(int value) {
        state.perFileChars = value;
    }

    public int getTotalChars() {
        return state.totalChars > 0 ? state.totalChars : DEFAULT_TOTAL_CHARS;
    }

    public void setTotalChars(int value) {
        state.totalChars = value;
    }

    public int getTimeoutSeconds() {
        return state.timeoutSeconds > 0 ? state.timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
    }

    public void setTimeoutSeconds(int value) {
        state.timeoutSeconds = value;
    }

    /**
     * 严格模式：生成结果会按 Conventional Commits 校验，不合规则自动重试一次。
     */
    public boolean isStrictMode() {
        return state.strictMode;
    }

    public void setStrictMode(boolean value) {
        state.strictMode = value;
    }

    /**
     * 判断用户是否填了 Base URL 与模型，用于给出更明确的提示。
     */
    public boolean hasEndpointConfigured() {
        return !getEffectiveBaseUrl().isEmpty() && !getEffectiveModel().isEmpty();
    }

    public static class State {
        public String provider = ProviderPreset.DEEPSEEK.name();
        public String baseUrl = "";
        public String model = "";
        public String messageStyle = MessageStyle.CONVENTIONAL_ZH.name();
        public String extraPrompt = "";
        public int perFileChars = DEFAULT_PER_FILE_CHARS;
        public int totalChars = DEFAULT_TOTAL_CHARS;
        public int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        public boolean strictMode = false;
    }

    @Nullable
    public static State newDefaultState() {
        return new State();
    }
}
