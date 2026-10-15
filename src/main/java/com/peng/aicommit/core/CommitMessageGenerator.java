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
        String systemPrompt = PromptBuilder.buildSystemPrompt(settings.getMessageStyle(), settings.getExtraPrompt());
        String userPrompt = PromptBuilder.buildUserPrompt(summary);

        indicator.setText("正在请求 " + provider.getDisplayName() + "…");
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(settings);
        String raw = client.complete(apiKey, systemPrompt, userPrompt, indicator);

        String message = MessageSanitizer.sanitize(raw, 0);
        if (message.isEmpty()) {
            throw new AiCommitException("模型返回了空的提交消息。");
        }
        return message;
    }
}
