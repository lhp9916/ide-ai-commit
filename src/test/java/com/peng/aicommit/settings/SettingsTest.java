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
}
