package com.wts.exam.controller;

import com.wts.common.result.R;
import com.wts.common.security.CurrentUser;
import com.wts.common.security.CurrentUserProvider;
import com.wts.exam.dto.BatchIdsDTO;
import com.wts.exam.dto.SubjectDTO;
import com.wts.exam.dto.SubjectQueryDTO;
import com.wts.exam.service.SubjectImportService;
import com.wts.exam.service.SubjectService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService service;
    private final SubjectImportService importService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public R<?> list(SubjectQueryDTO query) {
        return R.ok(service.list(query));
    }

    @GetMapping("/{id}")
    public R<?> detail(@PathVariable String id) {
        var subject = service.getDetail(id);
        var version = service.getCurrentVersion(id);
        var answers = version != null ? service.getVersionAnswers(version.getId()) : java.util.Collections.emptyList();
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("subject", subject);
        result.put("version", version);
        result.put("answers", answers);
        return R.ok(result);
    }

    @PostMapping
    public R<?> create(@RequestBody SubjectDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        return R.ok(service.create(dto, user.id(), user.displayName()));
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable String id, @RequestBody SubjectDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.update(id, dto, user.id(), user.displayName());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.delete(id, user.id());
        return R.ok();
    }

    @PostMapping("/batch-delete")
    public R<?> batchDelete(@RequestBody BatchIdsDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.deleteBatch(dto.normalizedIds(), user.id());
        return R.ok();
    }

    @PostMapping("/import")
    public R<?> importExcel(@RequestParam("file") MultipartFile file,
                            @RequestParam String typeid) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        try {
            return R.ok(importService.importFromExcel(file.getInputStream(), typeid, user.id(), user.displayName()));
        } catch (Exception e) {
            return R.fail("导入失败: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    public void exportExcel(HttpServletResponse response) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + URLEncoder.encode("题目导出.xlsx", StandardCharsets.UTF_8));
            importService.exportToExcel(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (Exception e) {
            throw com.wts.common.exception.BizException.fail("导出失败: " + e.getMessage());
        }
    }

    private void requireAdmin(CurrentUser user) {
        if (!user.isAdmin()) {
            throw com.wts.common.exception.BizException.fail("无权限操作");
        }
    }
}
