# Repository Guidelines

JetBrains IDE 插件：在提交消息框工具栏加一个按钮，分析已勾选的变更并调用大模型生成提交消息。

## Project Structure

`src/main/java/com/peng/aicommit/` 按职责分包：

- `ui/` — 动作与设置页
- `core/` — 变更收集、提示词、截断、清洗
- `llm/` — JSON 与 OpenAI 兼容客户端
- `settings/` — 设置持久化、密钥存储、供应商预设

`src/main/resources/META-INF/plugin.xml` 定义动作挂载点、设置页与通知组；`icons/` 放 SVG；`src/test/java/` 镜像主源码结构。

## Build, Test, and Development Commands

```powershell
$env:JAVA_HOME='D:\Program Files\JetBrains\GoLand 2022.1.4\jbr'
$env:GRADLE_USER_HOME="$PWD\.gradle-home"
.\gradlew.bat clean test buildPlugin
```

- `test` — 运行单元测试
- `buildPlugin` — 产出 `build/distributions/ide-ai-commit-<version>.zip`
- `runPluginVerifier "-PverifyLocal=<IDE 安装目录>"` — 离线校验兼容性，报告见 `build/reports/pluginVerifier/`

网络受限时追加 `"-Dhttp.proxyHost=127.0.0.1" "-Dhttp.proxyPort=10808"`。SDK 由 Gradle 下载 `ideaIC-2022.1`；不要改用 `localPath`，`Program Files` 无写权限会导致构建失败。

## Coding Style & Naming Conventions

- Java 11，4 空格缩进，UTF-8，`options.release=11`
- 包名 `com.peng.aicommit.*`，类名 `UpperCamelCase`，方法/字段 `lowerCamelCase`
- 注释与界面文案用中文，解释「为什么」而非「是什么」
- `core/`、`llm/` 保持零平台依赖，便于单测

## Testing Guidelines

- JUnit 4，测试类以 `Test` 结尾并与被测类同包
- 新增解析、截断、提示词、设置等纯逻辑必须补单测
- 运行：`.\gradlew.bat test`

## Commit & Pull Request Guidelines

- 遵循 Conventional Commits，描述用中文，如 `feat: 新增 AI 提交消息生成插件`
- 类型：`feat`、`fix`、`refactor`、`test`、`docs`、`chore`
- PR 说明改动目的与影响范围；UI 变更附截图；注明 `test` 与 `runPluginVerifier` 结果

## Compatibility & Security

- `since-build=221`，不设 `until-build`；改动平台 API 后需同时校验 2022.1 与新版 IDE（如 `DB-262`）
- 仅依赖 `platform` 与 `vcs` 模块，**不要**引入 Git4Idea
- API Key 只经 `PasswordSafe` 存储，禁止写入配置、日志或提交内容
