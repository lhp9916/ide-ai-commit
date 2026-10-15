package com.peng.aicommit.ui;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.vcs.commit.AbstractCommitWorkflowHandler;
import com.intellij.vcs.commit.CommitMessageUi;
import com.intellij.vcs.commit.CommitWorkflowHandler;
import com.peng.aicommit.AiCommitException;
import com.peng.aicommit.core.CommitMessageGenerator;
import com.peng.aicommit.settings.AiCommitSettings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 提交消息框工具栏上的按钮：分析已勾选的变更并生成提交消息。
 */
public class AiCommitAction extends DumbAwareAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        CommitMessageI messageControl = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);
        boolean enabled = project != null && messageControl != null && !collectChanges(e, project).isEmpty();
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        CommitMessageI messageControl = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);
        if (project == null || messageControl == null) {
            return;
        }

        List<Change> changes = collectChanges(e, project);
        if (changes.isEmpty()) {
            Notifications.warn(project, "没有可分析的变更，请先勾选要提交的文件。");
            return;
        }

        AiCommitSettings settings = AiCommitSettings.getInstance();
        CommitMessageGenerator generator = new CommitMessageGenerator(settings);
        setLoading(messageControl, true);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "AI 生成提交消息", true) {
            private String message;
            private AiCommitException failure;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    message = generator.generate(project, changes, indicator);
                } catch (AiCommitException ex) {
                    failure = ex;
                } catch (RuntimeException ex) {
                    failure = new AiCommitException("生成失败：" + ex.getMessage(), ex);
                }
            }

            @Override
            public void onFinished() {
                setLoading(messageControl, false);
                if (failure != null) {
                    Notifications.error(project, failure.getMessage());
                    return;
                }
                if (message != null && !message.isEmpty()) {
                    // 写入提交框，用户可直接 Ctrl+Z 撤销。
                    messageControl.setCommitMessage(message);
                }
            }
        });
    }

    /**
     * 优先取提交界面中已勾选的变更；上下文缺失时退化为所有本地变更。
     */
    private static List<Change> collectChanges(AnActionEvent e, Project project) {
        List<Change> included = new ArrayList<>();
        CommitWorkflowHandler workflowHandler = e.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER);
        if (workflowHandler instanceof AbstractCommitWorkflowHandler) {
            try {
                List<? extends Change> changes =
                        ((AbstractCommitWorkflowHandler) workflowHandler).getUi().getIncludedChanges();
                if (changes != null) {
                    included.addAll(changes);
                }
            } catch (RuntimeException ignored) {
                // 某些产品版本可能不提供该上下文，回退到全局变更。
            }
        }
        if (!included.isEmpty()) {
            return included;
        }
        Collection<Change> all = ChangeListManager.getInstance(project).getAllChanges();
        return new ArrayList<>(all);
    }

    /**
     * 复用平台提交框的加载动画；旧版本不支持时静默跳过。
     */
    private static void setLoading(CommitMessageI messageControl, boolean loading) {
        if (!(messageControl instanceof CommitMessageUi)) {
            return;
        }
        CommitMessageUi ui = (CommitMessageUi) messageControl;
        try {
            if (loading) {
                ui.startLoading();
            } else {
                ui.stopLoading();
            }
        } catch (RuntimeException ignored) {
            // 加载态属于体验增强，失败不影响主流程。
        }
    }
}
