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
        return R.ok(service.list(query, permissionService.visibleOwnerIds(user)));
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
        requireOwns(user, version.getCuser());
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

    /** 校验当前用户对该题目当前版本的归属（平台管理员不限） */
    private void requireOwnsVersion(CurrentUser user, String subjectId) {
        var version = service.getCurrentVersion(subjectId);
        if (version == null) {
            throw BizException.notFound("题目版本");
        }
        requireOwns(user, version.getCuser());
    }

    private void requireOwns(CurrentUser user, String ownerId) {
        if (user.isPlatformAdmin()) {
            return;
        }
        List<String> scope = permissionService.visibleOwnerIds(user);
        if (scope == null) {
            return;
        }
        if (ownerId == null || !scope.contains(ownerId)) {
            throw BizException.forbidden("只能操作自己创建的题目");
        }
    }
}