# AI Commit Message

JetBrains IDE 插件：在提交消息框工具栏加一个按钮，点击后分析已勾选的变更，调用大模型生成提交消息并直接填入提交框。

## 兼容性

- `since-build=221`，即 IntelliJ Platform 2022.1 及以上，不设 `until-build` 上界。
- 依赖 `com.intellij.modules.platform` 与 `com.intellij.modules.vcs`，**不依赖 Git4Idea**，因此对 IDEA / GoLand / PyCharm / WebStorm / DataGrip 等全系列 IDE 以及非 Git 的 VCS 同样可用。
- 已用 IntelliJ Plugin Verifier 实测：
  - GoLand 2022.1.4（`GO-221.6008.15`）：Compatible
  - DataGrip 2025.1.2（`DB-262.10315.24`）：Compatible

## 使用

1. 打开 `设置 > 工具 > AI Commit Message`。
2. 选择供应商，填写 API Key（Base URL 与模型会按供应商自动带出，可改）。
3. 点「测试连接」确认可用后保存。
4. 回到提交面板勾选要提交的文件，点击提交消息框右上角的 AI 图标即可生成。

生成的文本直接写入提交框，可用 `Ctrl+Z` 撤销。

## 内置供应商

均为 OpenAI 兼容的 `/chat/completions` 协议，Base URL 与模型可覆盖：

| 供应商 | 默认 Base URL | 默认模型 |
| --- | --- | --- |
| DeepSeek | `https://api.deepseek.com/v1` | `deepseek-chat` |
| 通义千问 DashScope | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `qwen-plus` |
| 智谱 GLM | `https://open.bigmodel.cn/api/paas/v4` | `glm-4-flash` |
| Kimi Moonshot | `https://api.moonshot.cn/v1` | `moonshot-v1-8k` |
| 硅基流动 SiliconFlow | `https://api.siliconflow.cn/v1` | `deepseek-ai/DeepSeek-V3` |
| 自定义 | 自行填写 | 自行填写 |

## 设置项

- **提交消息风格**：中文 Conventional Commits（默认）、中文纯描述、英文 Conventional Commits。
- **附加要求**：追加到系统提示词的额外约束，例如「不要使用 emoji」。
- **单文件差异上限 / 总差异上限**：默认 6000 / 24000 字符，超出时按文件截断并保留完整文件清单。
- **请求超时**：默认 60 秒。

API Key 通过 IDE 凭据库（`PasswordSafe`）加密存储，不写入明文配置；凭据库不可用时仅保留在内存中并在设置页提示。

## 构建

需要 JDK 11 及以上。Gradle 使用 wrapper（8.7）：

```powershell
$env:JAVA_HOME='D:\Program Files\JetBrains\GoLand 2022.1.4\jbr'
$env:GRADLE_USER_HOME="$PWD\.gradle-home"
.\gradlew.bat clean test buildPlugin
```

产物位于 `build/distributions/ide-ai-commit-<version>.zip`，在 IDE 中通过
`设置 > 插件 > 齿轮 > 从磁盘安装插件` 安装。

### 兼容性校验

用本机已安装的 IDE 做离线校验（无需下载 IDE 包）：

```powershell
.\gradlew.bat runPluginVerifier "-PverifyLocal=D:\Program Files\JetBrains\GoLand 2022.1.4"
```

报告输出在 `build/reports/pluginVerifier/`。

### 版本号

默认版本号写在 `build.gradle.kts`。发布时由 tag 派生，无需改文件：

```powershell
.\gradlew.bat buildPlugin "-PpluginVersion=1.2.3"
```

产物名为 `build/distributions/ide-ai-commit-1.2.3.zip`。

### 网络受限环境

依赖下载可通过代理：

```powershell
.\gradlew.bat buildPlugin "-Dhttp.proxyHost=127.0.0.1" "-Dhttp.proxyPort=10808" "-Dhttps.proxyHost=127.0.0.1" "-Dhttps.proxyPort=10808"
```

## 持续集成与发布

工作流位于 `.github/workflows/`：

| 工作流 | 触发条件 | 行为 |
| --- | --- | --- |
| `build.yml` | push 任意分支、PR、手动 | 跑单测并构建插件，上传 zip 与测试报告为构建产物 |
| `release.yml` | 推送 `v*` tag | 由 tag 派生版本号构建，创建 GitHub Release 并附带插件 zip |

发布新版本：

```powershell
git tag -a v0.2.0 -m "release: v0.2.0"
git push origin v0.2.0
```

推送后 `release.yml` 会自动产出 `ide-ai-commit-0.2.0.zip` 并挂到 Release 附件，用户可直接下载安装。

## 说明

- 插件 ID 为占位的 `com.peng.aicommit`，尚未配置发布签名。
- 界面文案为中文。
