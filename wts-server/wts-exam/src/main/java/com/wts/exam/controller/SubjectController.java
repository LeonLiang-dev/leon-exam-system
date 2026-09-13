package com.wts.exam.controller;

import com.wts.auth.enums.Permission;
import com.wts.auth.service.PermissionService;
import com.wts.common.exception.BizException;
import com.wts.common.result.R;
import com.wts.common.security.CurrentUser;
import com.wts.common.security.CurrentUserProvider;
import com.wts.exam.dto.BatchIdsDTO;
import com.wts.exam.dto.SubjectDTO;
import com.wts.exam.dto.SubjectQueryDTO;
import com.wts.exam.entity.ExamSubjectVersion;
import com.wts.exam.service.SubjectImportService;
import com.wts.exam.service.SubjectService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService service;
    private final SubjectImportService importService;
    private final CurrentUserProvider currentUserProvider;
    private final PermissionService permissionService;

    @GetMapping
    public R<?> list(SubjectQueryDTO query) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        // 题目全院共用：任何教职工可见全院题目（orgId 过滤仍生效），编辑/删除仍限创建者/主任/管理员
        return R.ok(service.list(query, null));
    }

    @GetMapping("/{id}")
    public R<?> detail(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        var subject = service.getDetail(id);
        var version = service.getCurrentVersion(id);
        if (version == null) {
            throw BizException.notFound("题目版本");
        }
        var answers = service.getVersionAnswers(version.getId());
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("subject", subject);
        result.put("version", version);
        result.put("answers", answers);
        return R.ok(result);
    }

    @PostMapping
    public R<?> create(@RequestBody SubjectDTO dto) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        return R.ok(service.create(dto, user.id(), user.displayName()));
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable String id, @RequestBody SubjectDTO dto) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        requireOwnsVersion(user, id);
        service.update(id, dto, user.id(), user.displayName());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        requireOwnsVersion(user, id);
        service.delete(id, user.id());
        return R.ok();
    }

    @PostMapping("/batch-delete")
    public R<?> batchDelete(@RequestBody BatchIdsDTO dto) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        List<String> ids = dto.normalizedIds();
        for (String id : ids) {
            requireOwnsVersion(user, id);
        }
        service.deleteBatch(ids, user.id());
        return R.ok();
    }

    @PostMapping("/import")
    public R<?> importExcel(@RequestParam("file") MultipartFile file,
                            @RequestParam String typeid) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        try {
            return R.ok(importService.importFromExcel(file.getInputStream(), typeid, user.id(), user.displayName()));
        } catch (Exception e) {
            return R.fail("导入失败: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    public void exportExcel(HttpServletResponse response) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + URLEncoder.encode("题目导出.xlsx", StandardCharsets.UTF_8));
            importService.exportToExcel(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (Exception e) {
            throw BizException.fail("导出失败: " + e.getMessage());
        }
    }

    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + URLEncoder.encode("题目导入模板.xlsx", StandardCharsets.UTF_8));
            importService.downloadTemplate(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (Exception e) {
            throw BizException.fail("下载模板失败: " + e.getMessage());
        }
    }

    /** 校验当前用户对该题目当前版本的归属（创建者/主任/副主任/平台管理员可编辑删除） */
    private void requireOwnsVersion(CurrentUser user, String subjectId) {
        var version = service.getCurrentVersion(subjectId);
        if (version == null) {
            throw BizException.notFound("题目版本");
        }
        requireEditor(user, version.getCuser());
    }

    /** 题目/试卷全院共用后，编辑/删除仅限创建者本人或教研室主任/平台管理员 */
    private void requireEditor(CurrentUser user, String ownerId) {
        if (user.isPlatformAdmin() || user.isDeptManager()) {
            return;
        }
        if (ownerId == null || !ownerId.equals(user.id())) {
            throw BizException.forbidden("只能操作自己创建的题目");
        }
    }
}