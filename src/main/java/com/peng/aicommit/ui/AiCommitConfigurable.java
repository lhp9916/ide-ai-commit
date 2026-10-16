package com.peng.aicommit.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.peng.aicommit.AiCommitException;
import com.peng.aicommit.core.MessageStyle;
import com.peng.aicommit.core.PromptBuilder;
import com.peng.aicommit.llm.OpenAiCompatibleClient;
import com.peng.aicommit.settings.AiCommitSettings;
import com.peng.aicommit.settings.ApiKeyStore;
import com.peng.aicommit.settings.PresetResolver;
import com.peng.aicommit.settings.ProviderPreset;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.util.List;

/**
 * Settings > Tools > AI Commit Message 配置页。
 */
public class AiCommitConfigurable implements Configurable {

    private final AiCommitSettings settings = AiCommitSettings.getInstance();

    private JPanel panel;
    private JComboBox<ProviderPreset> providerCombo;
    private JBTextField baseUrlField;
    /** 可编辑下拉框：既能手填，也能用「获取模型」填充该供应商的在线模型列表。 */
    private JComboBox<String> modelCombo;
    private JBPasswordField apiKeyField;
    private JCheckBox showKeyCheckBox;
    private JComboBox<MessageStyle> styleCombo;
    private JCheckBox strictModeCheckBox;
    private JBTextArea extraPromptArea;
    private JBTextField perFileCharsField;
    private JBTextField totalCharsField;
    private JBTextField timeoutField;
    private JButton testButton;
    private JButton fetchModelsButton;
    private JBLabel memoryOnlyHint;

    private ProviderPreset lastProvider;

    @Override
    public @Nls String getDisplayName() {
        return "AI Commit Message";
    }

    @Override
    public @Nullable JComponent createComponent() {
        providerCombo = new JComboBox<>(ProviderPreset.values());
        baseUrlField = new JBTextField();
        modelCombo = new JComboBox<>();
        modelCombo.setEditable(true);
        modelCombo.setPrototypeDisplayValue("deepseek-ai/DeepSeek-V4-Flash");
        apiKeyField = new JBPasswordField();
        apiKeyField.getEmptyText().setText("在此粘贴 API Key");
        showKeyCheckBox = new JCheckBox("显示密钥");
        styleCombo = new JComboBox<>(MessageStyle.values());
        strictModeCheckBox = new JCheckBox("严格模式（按 Conventional Commits 校验，不合规自动重写一次）");

        extraPromptArea = new JBTextArea(3, 40);
        extraPromptArea.setLineWrap(true);
        extraPromptArea.setWrapStyleWord(true);
        JBScrollPane extraPromptScroll = new JBScrollPane(extraPromptArea);

        perFileCharsField = new JBTextField();
        totalCharsField = new JBTextField();
        timeoutField = new JBTextField();

        testButton = new JButton("测试连接");
        testButton.addActionListener(e -> runConnectionTest());
        fetchModelsButton = new JButton("获取模型");
        fetchModelsButton.setToolTipText("从该供应商拉取当前可用模型，填充到左侧下拉框");
        fetchModelsButton.addActionListener(e -> runFetchModels());
        memoryOnlyHint = new JBLabel(" ");
        memoryOnlyHint.setForeground(com.intellij.ui.JBColor.RED);

        providerCombo.addActionListener(e -> onProviderChanged());
        showKeyCheckBox.addActionListener(e ->
                apiKeyField.setEchoChar(showKeyCheckBox.isSelected() ? (char) 0 : '\u2022'));

        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("供应商", providerCombo)
                .addLabeledComponent("Base URL", baseUrlField)
                .addLabeledComponent("模型", modelCombo)
                .addLabeledComponent("API Key", apiKeyField)
                .addComponent(showKeyCheckBox)
                .addComponent(memoryOnlyHint)
                .addComponent(fetchModelsButton)
                .addSeparator()
                .addLabeledComponent("提交消息风格", styleCombo)
                .addComponent(strictModeCheckBox)
                .addLabeledComponent("附加要求", extraPromptScroll)
                .addSeparator()
                .addLabeledComponent("单文件差异上限（字符）", perFileCharsField)
                .addLabeledComponent("总差异上限（字符）", totalCharsField)
                .addLabeledComponent("请求超时（秒）", timeoutField)
                .addComponent(testButton)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        panel.setPreferredSize(new Dimension(720, 460));
        reset();
        return panel;
    }

    /**
     * 模型名迭代很快，写死在插件里必然过时，因此提供按需拉取。
     */
    private void runFetchModels() {
        ProviderPreset provider = (ProviderPreset) providerCombo.getSelectedItem();
        if (provider == null) {
            return;
        }
        String baseUrl = normalize(baseUrlField.getText());
        String effectiveBaseUrl = baseUrl.isEmpty() ? provider.getDefaultBaseUrl() : baseUrl;
        String apiKey = new String(apiKeyField.getPassword()).trim();

        if (effectiveBaseUrl.isEmpty()) {
            Messages.showWarningDialog(panel, "请先填写 Base URL。", "AI Commit Message");
            return;
        }
        if (apiKey.isEmpty()) {
            Messages.showWarningDialog(panel, "请先填写 API Key，获取模型列表需要鉴权。", "AI Commit Message");
            return;
        }

        OpenAiCompatibleClient.EndpointConfig config = new OpenAiCompatibleClient.EndpointConfig(
                effectiveBaseUrl, currentModel(), parseTimeoutOr(settings.getTimeoutSeconds()));
        String current = currentModel();

        ProgressManager.getInstance().run(
                new Task.Backgroundable(ProjectManager.getInstance().getDefaultProject(), "获取模型列表", false) {
                    private List<String> models;
                    private AiCommitException failure;

                    @Override
                    public void run(ProgressIndicator indicator) {
                        try {
                            models = new OpenAiCompatibleClient(config).listModels(apiKey, indicator);
                        } catch (AiCommitException ex) {
                            failure = ex;
                        } catch (RuntimeException ex) {
                            failure = new AiCommitException("获取模型列表失败：" + ex.getMessage(), ex);
                        }
                    }

                    @Override
                    public void onFinished() {
                        if (failure != null) {
                            Messages.showErrorDialog(panel, failure.getMessage(), "AI Commit Message");
                            return;
                        }
                        applyModelList(models, current);
                    }
                });
    }

    /**
     * 用拉取到的模型刷新下拉框，并尽量保留用户当前选择。
     */
    private void applyModelList(List<String> models, String preferred) {
        if (models == null || models.isEmpty()) {
            Messages.showInfoMessage(panel, "该供应商未返回任何模型。", "AI Commit Message");
            return;
        }
        modelCombo.removeAllItems();
        for (String model : models) {
            modelCombo.addItem(model);
        }
        String target = null;
        if (preferred != null && models.contains(preferred)) {
            target = preferred;
        } else {
            target = models.get(0);
        }
        modelCombo.setSelectedItem(target);
        Messages.showInfoMessage(panel,
                "已获取 " + models.size() + " 个模型，当前选择：" + target, "AI Commit Message");
    }

    /**
     * 读取模型下拉框当前值，兼容可编辑状态下用户手输的内容。
     */
    private String currentModel() {
        Object editorValue = modelCombo.isEditable() ? modelCombo.getEditor().getItem() : null;
        Object value = editorValue != null ? editorValue : modelCombo.getSelectedItem();
        return value == null ? "" : value.toString().trim();
    }

    private void setCurrentModel(String model) {
        modelCombo.setSelectedItem(model == null ? "" : model);
        if (modelCombo.isEditable()) {
            modelCombo.getEditor().setItem(model == null ? "" : model);
        }
    }

    private void onProviderChanged() {
        ProviderPreset selected = (ProviderPreset) providerCombo.getSelectedItem();
        if (selected == null) {
            return;
        }
        // 仅在用户未自定义时，用新预设的默认值覆盖，避免丢失手填内容。
        baseUrlField.setText(PresetResolver.resolveBaseUrl(lastProvider, selected, baseUrlField.getText()));
        setCurrentModel(PresetResolver.resolveModel(lastProvider, selected, currentModel()));
        lastProvider = selected;
        apiKeyField.setText(ApiKeyStore.get(selected));
        updateMemoryOnlyHint(selected);
    }

    private void updateMemoryOnlyHint(ProviderPreset provider) {
        if (ApiKeyStore.isMemoryOnly(provider)) {
            memoryOnlyHint.setText("提示：凭据库不可用，该密钥仅保存在当前会话内存中，重启后需要重新输入。");
        } else {
            memoryOnlyHint.setText(" ");
        }
    }

    private void runConnectionTest() {
        ProviderPreset provider = (ProviderPreset) providerCombo.getSelectedItem();
        if (provider == null) {
            return;
        }
        String baseUrl = baseUrlField.getText() == null ? "" : baseUrlField.getText().trim();
        String model = currentModel();
        String effectiveBaseUrl = baseUrl.isEmpty() ? provider.getDefaultBaseUrl() : baseUrl;
        String effectiveModel = model.isEmpty() ? provider.getDefaultModel() : model;
        String apiKey = new String(apiKeyField.getPassword());

        if (effectiveBaseUrl.isEmpty() || effectiveModel.isEmpty()) {
            Messages.showWarningDialog(panel, "请先填写 Base URL 与模型名称。", "AI Commit Message");
            return;
        }
        if (apiKey.trim().isEmpty()) {
            Messages.showWarningDialog(panel, "请先填写 API Key。", "AI Commit Message");
            return;
        }

        // 直接用面板上的当前值测试，不必先保存设置。
        OpenAiCompatibleClient.EndpointConfig config = new OpenAiCompatibleClient.EndpointConfig(
                effectiveBaseUrl, effectiveModel, parseTimeoutOr(settings.getTimeoutSeconds()));

        Project project = ProjectManager.getInstance().getDefaultProject();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "测试 AI 服务连接", false) {
            @Override
            public void run(ProgressIndicator indicator) {
                try {
                    OpenAiCompatibleClient client = new OpenAiCompatibleClient(config);
                    String reply = client.complete(apiKey, "你是一个用于连通性测试的助手，请严格按要求回复。",
                            PromptBuilder.buildConnectionTestPrompt(), indicator);
                    ApplicationManager.getApplication().invokeLater(() ->
                            Messages.showInfoMessage(panel, "连接成功，模型回复：" + reply, "AI Commit Message"));
                } catch (AiCommitException ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Messages.showErrorDialog(panel, ex.getMessage(), "AI Commit Message"));
                } catch (RuntimeException ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Messages.showErrorDialog(panel, "测试失败：" + ex.getMessage(), "AI Commit Message"));
                }
            }
        });
    }

    private int parseTimeoutOr(int fallback) {
        try {
            int value = Integer.parseInt(normalize(timeoutField.getText()));
            return value >= 5 ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public boolean isModified() {
        ProviderPreset provider = (ProviderPreset) providerCombo.getSelectedItem();
        MessageStyle style = (MessageStyle) styleCombo.getSelectedItem();
        String key = new String(apiKeyField.getPassword());
        return provider != settings.getProvider()
                || !equalsText(baseUrlField.getText(), settings.getRawBaseUrl())
                || !equalsText(currentModel(), settings.getRawModel())
                || style != settings.getMessageStyle()
                || strictModeCheckBox.isSelected() != settings.isStrictMode()
                || !equalsText(extraPromptArea.getText(), settings.getExtraPrompt())
                || !equalsInt(perFileCharsField.getText(), settings.getPerFileChars())
                || !equalsInt(totalCharsField.getText(), settings.getTotalChars())
                || !equalsInt(timeoutField.getText(), settings.getTimeoutSeconds())
                || provider != null && !key.equals(ApiKeyStore.get(provider));
    }

    private static boolean equalsText(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean equalsInt(String text, int expected) {
        try {
            return Integer.parseInt(normalize(text)) == expected;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void apply() throws ConfigurationException {
        ProviderPreset provider = (ProviderPreset) providerCombo.getSelectedItem();
        MessageStyle style = (MessageStyle) styleCombo.getSelectedItem();
        if (provider == null || style == null) {
            return;
        }
        int perFile = parsePositive(perFileCharsField.getText(), "单文件差异上限",
                AiCommitSettings.DEFAULT_PER_FILE_CHARS);
        int total = parsePositive(totalCharsField.getText(), "总差异上限",
                AiCommitSettings.DEFAULT_TOTAL_CHARS);
        int timeout = parsePositive(timeoutField.getText(), "请求超时",
                AiCommitSettings.DEFAULT_TIMEOUT_SECONDS);
        if (timeout < 5) {
            throw new ConfigurationException("请求超时不能小于 5 秒。");
        }

        settings.setProvider(provider);
        settings.setRawBaseUrl(normalize(baseUrlField.getText()));
        settings.setRawModel(currentModel());
        settings.setMessageStyle(style);
        settings.setStrictMode(strictModeCheckBox.isSelected());
        settings.setExtraPrompt(extraPromptArea.getText() == null ? "" : extraPromptArea.getText());
        settings.setPerFileChars(perFile);
        settings.setTotalChars(total);
        settings.setTimeoutSeconds(timeout);

        ApiKeyStore.set(provider, new String(apiKeyField.getPassword()).trim());
        updateMemoryOnlyHint(provider);
    }

    private static int parsePositive(String text, String fieldName, int fallback) throws ConfigurationException {
        String value = normalize(text);
        if (value.isEmpty()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new ConfigurationException(fieldName + "必须为正整数。");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new ConfigurationException(fieldName + "必须为整数。");
        }
    }

    @Override
    public void reset() {
        ProviderPreset provider = settings.getProvider();
        lastProvider = provider;
        providerCombo.setSelectedItem(provider);
        baseUrlField.setText(settings.getRawBaseUrl());
        setCurrentModel(settings.getRawModel());
        apiKeyField.setText(ApiKeyStore.get(provider));
        styleCombo.setSelectedItem(settings.getMessageStyle());
        strictModeCheckBox.setSelected(settings.isStrictMode());
        extraPromptArea.setText(settings.getExtraPrompt());
        perFileCharsField.setText(String.valueOf(settings.getPerFileChars()));
        totalCharsField.setText(String.valueOf(settings.getTotalChars()));
        timeoutField.setText(String.valueOf(settings.getTimeoutSeconds()));
        updateMemoryOnlyHint(provider);
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        providerCombo = null;
        baseUrlField = null;
        modelCombo = null;
        apiKeyField = null;
        showKeyCheckBox = null;
        styleCombo = null;
        strictModeCheckBox = null;
        extraPromptArea = null;
        perFileCharsField = null;
        totalCharsField = null;
        timeoutField = null;
        testButton = null;
        memoryOnlyHint = null;
    }

}
