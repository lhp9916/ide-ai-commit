plugins {
    java
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.peng"
// 版本号优先级：-PpluginVersion（CI 由 tag 派生）> 默认值。
// 不用 -Pversion 是因为它会被 Gradle 自身的 version 属性吞掉。
version = (findProperty("pluginVersion") as String?)?.removePrefix("v")?.takeIf { it.isNotBlank() } ?: "0.1.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2022.1")
    type.set("IC")
    // 保持 plugin.xml 中的 since-build，不自动写入 until-build，以便兼容更高版本 IDE。
    updateSinceUntilBuild.set(false)
    downloadSources.set(false)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(11)
    }

    test {
        useJUnit()
        testLogging {
            events("passed", "failed", "skipped")
        }
    }

    // 本插件不提供可被 Settings 搜索框检索的配置项，关闭该任务以加快构建。
    named("buildSearchableOptions") {
        enabled = false
    }

    withType<org.jetbrains.intellij.tasks.RunPluginVerifierTask> {
        downloadDir.set(File(System.getProperty("java.io.tmpdir"), "pluginVerifierCache").absolutePath)
        // 通过 -PverifyLocal=<IDE 安装目录> 用本机 IDE 做离线兼容性校验。
        providers.gradleProperty("verifyLocal").orNull?.let { path -> localPaths.set(listOf(file(path))) }
        // 描述文案为中文，属发布元数据而非 API 兼容问题，此处不参与校验失败判定。
        failureLevel.set(listOf(
            org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.MISSING_DEPENDENCIES,
            org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.INVALID_PLUGIN,
            org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.INTERNAL_API_USAGES,
            org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.OVERRIDE_ONLY_API_USAGES,
            org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.NON_EXTENDABLE_API_USAGES
        ))
    }
}
