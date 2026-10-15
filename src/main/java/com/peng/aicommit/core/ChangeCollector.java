package com.peng.aicommit.core;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diff.impl.patch.BinaryFilePatch;
import com.intellij.openapi.diff.impl.patch.FilePatch;
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder;
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangesUtil;
import com.intellij.openapi.vcs.changes.CommitContext;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把 Change 列表转换为按文件组织的差异文本。
 * 使用平台补丁构建器，因此与具体 VCS 类型无关。
 */
public final class ChangeCollector {
    private ChangeCollector() {
    }

    /**
     * 收集差异片段。整个过程在读锁下执行。
     */
    public static List<FileSection> collect(Project project, Collection<Change> changes) {
        if (changes == null || changes.isEmpty()) {
            return new ArrayList<>();
        }
        List<Change> ordered = new ArrayList<>(changes);
        return ApplicationManager.getApplication().runReadAction(
                (com.intellij.openapi.util.Computable<List<FileSection>>) () -> build(project, ordered));
    }

    private static List<FileSection> build(Project project, List<Change> changes) {
        String basePath = project.getBasePath();
        List<FilePatch> patches;
        try {
            patches = IdeaTextPatchBuilder.buildPatch(project, changes, basePath, false);
        } catch (Exception e) {
            // 构建补丁失败时退化为仅提供文件清单。
            return fallbackSections(changes);
        }

        Map<String, FileSection> byPath = new LinkedHashMap<>();
        for (FilePatch patch : patches) {
            String path = displayPath(patch);
            String status = statusOf(patch);
            if (patch instanceof BinaryFilePatch) {
                byPath.put(path, new FileSection(path, status, true, ""));
                continue;
            }
            byPath.put(path, new FileSection(path, status, false, renderSinglePatch(project, patch)));
        }

        // 补上构建补丁时被跳过的文件（例如二进制或无法读取的内容）。
        for (Change change : changes) {
            String path = pathOf(change);
            if (path != null && !byPath.containsKey(path)) {
                byPath.put(path, new FileSection(path, "已修改", false, ""));
            }
        }
        return new ArrayList<>(byPath.values());
    }

    private static String renderSinglePatch(Project project, FilePatch patch) {
        try {
            StringWriter writer = new StringWriter();
            List<FilePatch> single = new ArrayList<>();
            single.add(patch);
            UnifiedDiffWriter.write(project, single, writer, "\n", new CommitContext());
            return writer.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static List<FileSection> fallbackSections(List<Change> changes) {
        List<FileSection> sections = new ArrayList<>();
        for (Change change : changes) {
            String path = pathOf(change);
            if (path != null) {
                sections.add(new FileSection(path, "已修改", false, ""));
            }
        }
        return sections;
    }

    private static String displayPath(FilePatch patch) {
        if (patch.isNewFile()) {
            return normalize(patch.getAfterFileName());
        }
        if (patch.isDeletedFile()) {
            return normalize(patch.getBeforeFileName());
        }
        return normalize(patch.getAfterFileName());
    }

    private static String statusOf(FilePatch patch) {
        if (patch.isNewFile()) {
            return "新增";
        }
        if (patch.isDeletedFile()) {
            return "删除";
        }
        return "修改";
    }

    private static String pathOf(Change change) {
        try {
            FilePath path = ChangesUtil.getFilePath(change);
            return path == null ? null : normalize(path.getPath());
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalize(String path) {
        return path == null ? "(未知文件)" : path.replace('\\', '/');
    }
}
