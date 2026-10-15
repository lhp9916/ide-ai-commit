package com.peng.aicommit.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;

/**
 * 统一的通知入口，保证分组 id 与 plugin.xml 中注册的一致。
 */
public final class Notifications {
    public static final String GROUP_ID = "AI Commit Message";

    private Notifications() {
    }

    public static void error(Project project, String message) {
        show(project, "AI 生成提交消息失败", message, NotificationType.ERROR, true);
    }

    public static void warn(Project project, String message) {
        show(project, "AI 生成提交消息", message, NotificationType.WARNING, false);
    }

    public static void info(Project project, String message) {
        show(project, "AI 生成提交消息", message, NotificationType.INFORMATION, false);
    }

    private static void show(Project project, String title, String content, NotificationType type,
                             boolean withSettingsAction) {
        NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup(GROUP_ID);
        if (group == null) {
            return;
        }
        Notification notification = group.createNotification(title, content, type);
        if (withSettingsAction) {
            notification.addAction(NotificationAction.createSimple("打开设置", () ->
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, AiCommitConfigurable.class)));
        }
        com.intellij.notification.Notifications.Bus.notify(notification, project);
    }
}
