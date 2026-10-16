package com.peng.aicommit.core;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.peng.aicommit.AiCommitException;
import com.peng.aicommit.llm.OpenAiCompatibleClient;
import com.peng.aicommit.settings.AiCommitSettings;
import com.peng.aicommit.settings.ApiKeyStore;
import com.peng.aicommit.settings.ProviderPreset;

import java.util.Collection;
import java.util.List;

/**
 * 串联「收集变更 -> 组装提示词 -> 调用模型 -> 清洗结果」的完整流程。
 */
public class CommitMessageGenerator {

    private final AiCommitSettings settings;

    public CommitMessageGenerator(AiCommitSettings settings) {
        this.settings = settings;
    }

    /**
     * 生成提交消息。所有耗时操作都应在后台线程调用。
     */
    public String generate(Project project, Collection<Change> changes, ProgressIndicator indicator)
            throws AiCommitException {
        ProviderPreset provider = settings.getProvider();
        String apiKey = ApiKeyStore.get(provider).trim();
        if (apiKey.isEmpty()) {
            throw new AiCommitException("尚未配置 API Key，请在「设置 > 工具 > AI Commit Message」中填写。");
        }
        if (!settings.hasEndpointConfigured()) {
            throw new AiCommitException("尚未配置 Base URL 或模型名称，请先在设置中补全。");
        }

        indicator.setText("正在分析变更…");
        List<FileSection> sections = ChangeCollector.collect(project, changes);
        if (sections.isEmpty()) {
            throw new AiCommitException("没有可分析的变更。");
        }

        String summary = PatchTruncator.render(sections, settings.getPerFileChars(), settings.getTotalChars());
        String userPrompt = PromptBuilder.buildUserPrompt(summary);

        indicator.setText("正在请求 " + provider.getDisplayName() + "…");
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(settings);
        MessageStyle style = settings.getMessageStyle();
        boolean strict = settings.isStrictMode() && style != MessageStyle.PLAIN_ZH;

        String systemPrompt = PromptBuilder.buildSystemPrompt(style, settings.getExtraPrompt(), strict);
        String raw = client.complete(apiKey, systemPrompt, userPrompt, indicator);
        String message = sanitizeOrThrow(raw);

        if (strict) {
            message = enforceConventional(client, apiKey, indicator, style, message);
        }
        return message;
    }

    /**
     * 严格模式：校验不符合规范时把问题回喂给模型重写一次；
     * 若重写仍不合规，则返回更接近规范的那一版，不阻塞用户。
     */
    private String enforceConventional(OpenAiCompatibleClient client, String apiKey,
                                       ProgressIndicator indicator, MessageStyle style, String firstAttempt) {
        List<String> problems = CommitMessageValidator.validate(firstAttempt);
        if (problems.isEmpty()) {
            return firstAttempt;
        }

        indicator.setText("正在按规范修正提交消息…");
        String repairPrompt = PromptBuilder.buildRepairPrompt(firstAttempt, problems);
        try {
            String rawRetry = client.complete(apiKey,
                    PromptBuilder.buildSystemPrompt(style, settings.getExtraPrompt(), true),
                    repairPrompt, indicator);
            String retry = sanitizeOrThrow(rawRetry);
            // 仅在重写确实减少了问题数时才采用，避免越改越差。
            return CommitMessageValidator.validate(retry).size() <= problems.size() ? retry : firstAttempt;
        } catch (AiCommitException e) {
            // 重写失败不应让整个生成失败，保留首次结果。
            return firstAttempt;
        }
    }

    private static String sanitizeOrThrow(String raw) {
        String message = MessageSanitizer.sanitize(raw, 0);
        if (message.isEmpty()) {
            throw new AiCommitException("模型返回了空的提交消息。");
        }
        return message;
    }
}
