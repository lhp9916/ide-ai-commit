package com.peng.aicommit.settings;

import com.peng.aicommit.core.MessageStyle;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SettingsTest {

    @Test
    public void presetResolverReplacesUntouchedDefault() {
        String resolved = PresetResolver.resolveBaseUrl(ProviderPreset.DEEPSEEK, ProviderPreset.ZHIPU,
                ProviderPreset.DEEPSEEK.getDefaultBaseUrl());
        assertEquals(ProviderPreset.ZHIPU.getDefaultBaseUrl(), resolved);
    }

    @Test
    public void presetResolverKeepsCustomValue() {
        String custom = "https://my.gateway/v1";
        String resolved = PresetResolver.resolveBaseUrl(ProviderPreset.DEEPSEEK, ProviderPreset.ZHIPU, custom);
        assertEquals(custom, resolved);
    }

    @Test
    public void presetResolverFillsWhenEmpty() {
        assertEquals(ProviderPreset.MOONSHOT.getDefaultBaseUrl(),
                PresetResolver.resolveBaseUrl(null, ProviderPreset.MOONSHOT, ""));
        assertEquals(ProviderPreset.MOONSHOT.getDefaultModel(),
                PresetResolver.resolveModel(null, ProviderPreset.MOONSHOT, "  "));
    }

    @Test
    public void styleRoundTripsThroughName() {
        assertEquals(MessageStyle.PLAIN_ZH, MessageStyle.fromName("PLAIN_ZH"));
        assertEquals(MessageStyle.PLAIN_ZH, MessageStyle.fromName("plain_zh"));
        assertEquals(MessageStyle.CONVENTIONAL_ZH, MessageStyle.fromName("unknown"));
        assertEquals(MessageStyle.CONVENTIONAL_ZH, MessageStyle.fromName(null));
    }

    @Test
    public void providerRoundTripsThroughIdAndName() {
        assertEquals(ProviderPreset.ZHIPU, ProviderPreset.fromName("ZHIPU"));
        assertEquals(ProviderPreset.ZHIPU, ProviderPreset.fromName("zhipu"));
        assertEquals(ProviderPreset.DEEPSEEK, ProviderPreset.fromName("nope"));
    }

    @Test
    public void stateDefaultsAreUsable() {
        AiCommitSettings.State state = new AiCommitSettings.State();
        assertEquals(ProviderPreset.DEEPSEEK.name(), state.provider);
        assertEquals(MessageStyle.CONVENTIONAL_ZH.name(), state.messageStyle);
        assertTrue(state.perFileChars > 0);
        assertTrue(state.totalChars > 0);
        assertTrue(state.timeoutSeconds > 0);
        assertFalse("严格模式默认关闭，避免多一次请求", state.strictMode);
    }

    @Test
    public void presetsExposeEndpointForEveryBuiltInProvider() {
        for (ProviderPreset preset : ProviderPreset.values()) {
            if (preset == ProviderPreset.CUSTOM) {
                assertTrue(preset.getDefaultBaseUrl().isEmpty());
                continue;
            }
            assertFalse(preset.getDefaultBaseUrl().isEmpty());
            assertFalse(preset.getDefaultModel().isEmpty());
        }
    }

    /**
     * DeepSeek 官方给出的 OpenAI 兼容 base_url 不带 /v1，若误加会导致 404，
     * 这里固定住以免以后被改回去。
     */
    @Test
    public void deepSeekBaseUrlHasNoV1Suffix() {
        assertEquals("https://api.deepseek.com", ProviderPreset.DEEPSEEK.getDefaultBaseUrl());
        assertEquals("deepseek-flash", ProviderPreset.DEEPSEEK.getDefaultModel());
    }

    @Test
    public void builtInProvidersUseCurrentModelNames() {
        assertEquals("qwen3.8-flash", ProviderPreset.DASHSCOPE.getDefaultModel());
        assertEquals("glm-5.3-flash", ProviderPreset.ZHIPU.getDefaultModel());
        assertEquals("kimi-k2.6", ProviderPreset.MOONSHOT.getDefaultModel());
        assertEquals("deepseek-ai/DeepSeek-V4-Flash", ProviderPreset.SILICONFLOW.getDefaultModel());
    }

    /**
     * 默认值统一取各家轻量/高速档：生成提交消息是短任务，无需旗舰模型。
     */
    @Test
    public void builtInProvidersDefaultToFastTierModels() {
        assertTrue(ProviderPreset.DEEPSEEK.getDefaultModel().contains("flash"));
        assertTrue(ProviderPreset.DASHSCOPE.getDefaultModel().contains("flash"));
        assertTrue(ProviderPreset.ZHIPU.getDefaultModel().contains("flash"));
        assertTrue(ProviderPreset.SILICONFLOW.getDefaultModel().contains("Flash"));
    }

    @Test
    public void builtInProvidersDoNotUseDeprecatedModelNames() {
        for (ProviderPreset preset : ProviderPreset.values()) {
            String model = preset.getDefaultModel();
            assertFalse(preset.getDisplayName() + " 仍在使用过时模型名 " + model,
                    model.equals("deepseek-chat") || model.equals("qwen-plus")
                            || model.equals("glm-4-flash") || model.equals("moonshot-v1-8k")
                            || model.equals("deepseek-ai/DeepSeek-V3"));
        }
    }
}
