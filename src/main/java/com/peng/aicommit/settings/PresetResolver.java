package com.peng.aicommit.settings;

/**
 * 切换供应商时决定 Base URL 与模型输入框的新值。
 * 仅在用户尚未自定义（为空或仍等于上一预设默认值）时才覆盖，避免丢失手填内容。
 */
public final class PresetResolver {
    private PresetResolver() {
    }

    public static String resolveBaseUrl(ProviderPreset previous, ProviderPreset next, String currentBaseUrl) {
        if (isUntouched(currentBaseUrl, previous == null ? null : previous.getDefaultBaseUrl())) {
            return next.getDefaultBaseUrl();
        }
        return currentBaseUrl;
    }

    public static String resolveModel(ProviderPreset previous, ProviderPreset next, String currentModel) {
        if (isUntouched(currentModel, previous == null ? null : previous.getDefaultModel())) {
            return next.getDefaultModel();
        }
        return currentModel;
    }

    private static boolean isUntouched(String current, String previousDefault) {
        if (current == null || current.trim().isEmpty()) {
            return true;
        }
        return previousDefault != null && previousDefault.equals(current.trim());
    }
}
