package com.wts.auth.controller;

import com.wts.auth.dto.StudentImportResult;
import com.wts.auth.dto.BatchIdsDTO;
import com.wts.auth.dto.UserDTO;
import com.wts.auth.entity.SysUser;
import com.wts.auth.enums.Permission;
import com.wts.auth.service.PermissionService;
import com.wts.auth.service.UserService;
import com.wts.common.exception.BizException;
import com.wts.common.result.PageResult;
import com.wts.common.result.R;
import com.wts.common.security.CurrentUser;
import com.wts.common.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public R<PageResult<SysUser>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String post,
            @RequestParam(required = false) String className) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.USER_MANAGE.name());
        PageResult<SysUser> result = userService.listUsers(
                page, size, keyword, state, post, className, permissionService.visibleUserIds(user));
        return R.ok(result);
    }

    @PostMapping
    public R<SysUser> create(@RequestBody UserDTO dto) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.USER_MANAGE.name());
        ensureOrgAllowed(user, dto.getOrgId());
        SysUser created = userService.createUser(dto, user.id());
        return R.ok(created);
    }

    @PostMapping("/import-students")
    public R<StudentImportResult> importStudents(@RequestParam("file") MultipartFile file) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.CLASS_IMPORT.name());
        if (file == null || file.isEmpty()) {
            throw BizException.fail("请上传学生帐号 Excel 文件");
        }
        try {
            return R.ok(userService.importStudents(file.getInputStream(), user.id()));
        } catch (IOException e) {
            throw BizException.fail("读取上传文件失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public R<SysUser> update(@PathVariable String id, @RequestBody UserDTO dto) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.USER_MANAGE.name());
        permissionService.ensureTargetsInScope(user, List.of(id));
        ensureOrgAllowed(user, dto.getOrgId());
        SysUser updated = userService.updateUser(id, dto, user.id());
        return R.ok(updated);
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.USER_MANAGE.name());
        permissionService.ensureTargetsInScope(user, List.of(id));
        userService.deleteUser(id, user.id());
        return R.ok();
    }

    @PostMapping("/batch-disable")
    public R<Void> batchDisable(@RequestBody BatchIdsDTO dto) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.USER_MANAGE.name());
        permissionService.ensureTargetsInScope(user, dto.getIds());
        userService.disableUsers(dto.getIds(), user.id());
        return R.ok();
    }

    @DeleteMapping("/{id}/hard-delete")
    public R<Void> hardDelete(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.USER_MANAGE.name());
        permissionService.ensureTargetsInScope(user, List.of(id));
        userService.hardDeleteUser(id, user.id());
        return R.ok();
    }

    @PostMapping("/batch-hard-delete")
    public R<Void> batchHardDelete(@RequestBody BatchIdsDTO dto) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.USER_MANAGE.name());
        permissionService.ensureTargetsInScope(user, dto.getIds());
        userService.hardDeleteUsers(dto.getIds(), user.id());
        return R.ok();
    }

    @PostMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.USER_MANAGE.name());
        permissionService.ensureTargetsInScope(user, List.of(id));
        userService.resetPassword(id, user.id());
        return R.ok();
    }

    @PostMapping("/change-password")
    public R<Void> changePassword(@RequestBody Map<String, String> body) {
        CurrentUser user = currentUserProvider.require();
        userService.changePassword(user.id(), body.get("oldPassword"), body.get("newPassword"));
        return R.ok();
    }

    /** 组织归属校验：非平台管理员只能将用户分配到自己可见的组织节点下 */
    private void ensureOrgAllowed(CurrentUser user, String orgId) {
        if (orgId == null || orgId.isBlank() || user.isPlatformAdmin()) {
            return;
        }
        List<String> visibleOrgs = permissionService.visibleOrgIds(user);
        if (visibleOrgs == null) {
            return;
        }
        Set<String> allowed = Set.copyOf(visibleOrgs);
        if (!allowed.contains(orgId)) {
            throw BizException.forbidden("目标组织不在你的管理范围内");
        }
    }
}