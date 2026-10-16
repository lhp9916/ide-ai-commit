package com.peng.aicommit.core;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommitMessageValidatorTest {

    @Test
    public void acceptsMinimalValidMessage() {
        assertTrue(CommitMessageValidator.validate("fix: 修复登录超时").isEmpty());
    }

    @Test
    public void acceptsScopeAndBody() {
        String message = "feat(upload): 添加文件信息日志记录\n"
                + "\n"
                + "- 在调用 RPC 前打印文件信息\n"
                + "- 便于排查上传问题";
        assertTrue(CommitMessageValidator.validate(message).isEmpty());
    }

    @Test
    public void acceptsBreakingChangeWithBangAndFooter() {
        String message = "feat(api)!: 移除已废弃的 v1 接口\n"
                + "\n"
                + "- 统一走 v2 接口\n"
                + "\n"
                + "BREAKING CHANGE: v1 接口已移除，请迁移到 v2";
        assertTrue(CommitMessageValidator.validate(message).isEmpty());
    }

    @Test
    public void acceptsRevertType() {
        assertTrue(CommitMessageValidator.validate("revert: 回退文件上传改动").isEmpty());
    }

    @Test
    public void rejectsUnknownType() {
        List<String> problems = CommitMessageValidator.validate("feet: 新增功能");
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("不在允许范围内"));
    }

    @Test
    public void rejectsMissingTypePrefix() {
        List<String> problems = CommitMessageValidator.validate("修复登录超时");
        assertTrue(problems.get(0).contains("必须符合"));
    }

    @Test
    public void rejectsTrailingPeriod() {
        List<String> problems = CommitMessageValidator.validate("fix: 修复登录超时。");
        assertTrue(problems.stream().anyMatch(p -> p.contains("句号")));
    }

    @Test
    public void rejectsUppercaseScope() {
        List<String> problems = CommitMessageValidator.validate("fix(uploadFile): 修复上传");
        assertTrue(problems.stream().anyMatch(p -> p.contains("小写")));
    }

    @Test
    public void rejectsEmptyScope() {
        List<String> problems = CommitMessageValidator.validate("fix(): 修复上传");
        assertTrue(problems.stream().anyMatch(p -> p.contains("空括号")));
    }

    @Test
    public void rejectsMissingBlankLineBeforeBody() {
        String message = "feat(upload): 添加日志\n- 在调用 RPC 前打印日志";
        List<String> problems = CommitMessageValidator.validate(message);
        assertTrue(problems.stream().anyMatch(p -> p.contains("空一行")));
    }

    @Test
    public void rejectsOverlongHeader() {
        StringBuilder longDescription = new StringBuilder();
        for (int i = 0; i < 120; i++) {
            longDescription.append('a');
        }
        List<String> problems = CommitMessageValidator.validate("feat: " + longDescription);
        assertTrue(problems.stream().anyMatch(p -> p.contains("100")));
    }

    @Test
    public void rejectsLowercaseBreakingChangeFooter() {
        String message = "feat(api)!: 移除 v1 接口\n"
                + "\n"
                + "- 统一走 v2\n"
                + "\n"
                + "breaking change: v1 已移除";
        List<String> problems = CommitMessageValidator.validate(message);
        assertTrue(problems.stream().anyMatch(p -> p.contains("大写")));
    }

    @Test
    public void reportsEmptyMessage() {
        assertEquals(1, CommitMessageValidator.validate("   ").size());
        assertEquals(1, CommitMessageValidator.validate(null).size());
    }

    @Test
    public void acceptsDescriptionContainingColon() {
        assertTrue(CommitMessageValidator.validate("fix: 修复 time: 格式解析错误").isEmpty());
    }

    @Test
    public void acceptsIssueFooter() {
        String message = "fix(upload): 修复超时\n\n- 调大超时时间\n\nCloses: #123";
        assertTrue(CommitMessageValidator.validate(message).isEmpty());
    }
}
