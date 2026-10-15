package com.peng.aicommit.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.diagnostic.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API Key 存储。优先写入 IDE 加密凭据库；凭据库不可用时退化为进程内存，
 * 此时不会落盘，并在界面上提示用户。
 */
public final class ApiKeyStore {
    private static final Logger LOG = Logger.getInstance(ApiKeyStore.class);
    private static final String SERVICE_PREFIX = "AI Commit Message";

    /** 凭据库不可用时的临时缓存，进程退出即失效。 */
    private static final Map<String, String> MEMORY_ONLY = new ConcurrentHashMap<>();

    private ApiKeyStore() {
    }

    public static String get(ProviderPreset provider) {
        String id = provider.getId();
        try {
            String value = PasswordSafe.getInstance().getPassword(attributes(provider));
            if (value != null && !value.isEmpty()) {
                return value;
            }
        } catch (Throwable t) {
            LOG.warn("读取凭据库失败，改用内存缓存", t);
        }
        return MEMORY_ONLY.getOrDefault(id, "");
    }

    public static void set(ProviderPreset provider, String apiKey) {
        String id = provider.getId();
        String value = apiKey == null ? "" : apiKey;
        try {
            PasswordSafe.getInstance().set(attributes(provider),
                    value.isEmpty() ? null : new Credentials(id, value));
            MEMORY_ONLY.remove(id);
            return;
        } catch (Throwable t) {
            LOG.warn("写入凭据库失败，仅保存在内存中", t);
        }
        if (value.isEmpty()) {
            MEMORY_ONLY.remove(id);
        } else {
            MEMORY_ONLY.put(id, value);
        }
    }

    /**
     * 判断某个 Key 当前是否只存在于内存中（即未能写入凭据库）。
     */
    public static boolean isMemoryOnly(ProviderPreset provider) {
        return MEMORY_ONLY.containsKey(provider.getId());
    }

    private static CredentialAttributes attributes(ProviderPreset provider) {
        // 使用两参构造：三参构造在较新平台已标记为弃用，两参版本在 2022.1 起即可用。
        return new CredentialAttributes(SERVICE_PREFIX + " / " + provider.getDisplayName(), provider.getId());
    }
}
