package com.peng.aicommit;

import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 校验 plugin.xml 中声明的类都能被加载。
 * 包路径写错时 IDE 启动会在设置页抛 ClassNotFoundException，
 * 这类错误编译期发现不了，因此在这里兜住。
 */
public class PluginXmlTest {

    private static final Pattern CLASS_REF = Pattern.compile(
            "(?:class|instance|serviceImplementation|implementation)=\"([A-Za-z_$][\\w.$]*)\"");

    @Test
    public void everyReferencedClassExists() throws Exception {
        String xml = readPluginXml();
        List<String> missing = new ArrayList<>();
        List<String> checked = new ArrayList<>();

        Matcher matcher = CLASS_REF.matcher(xml);
        while (matcher.find()) {
            String className = matcher.group(1);
            if (!className.contains(".")) {
                continue;
            }
            checked.add(className);
            try {
                Class.forName(className, false, getClass().getClassLoader());
            } catch (ClassNotFoundException e) {
                missing.add(className);
            }
        }

        assertTrue("plugin.xml 中未找到任何类引用，测试可能失效", !checked.isEmpty());
        assertTrue("plugin.xml 引用了不存在的类：" + missing, missing.isEmpty());
    }

    @Test
    public void actionIsMountedOnCommitMessageToolbar() throws Exception {
        String xml = readPluginXml();
        assertTrue("动作必须挂到 Vcs.MessageActionGroup，才会出现在提交消息框工具栏",
                xml.contains("group-id=\"Vcs.MessageActionGroup\""));
    }

    @Test
    public void declaresCompatibleSinceBuildAndNoUpperBound() throws Exception {
        String xml = readPluginXml();
        assertTrue("需声明 since-build=221 以兼容 2022.1", xml.contains("since-build=\"221\""));
        assertTrue("不应设置 until-build，否则新版 IDE 无法安装", !xml.contains("until-build"));
    }

    private String readPluginXml() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/META-INF/plugin.xml")) {
            assertNotNull("找不到 /META-INF/plugin.xml", in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
