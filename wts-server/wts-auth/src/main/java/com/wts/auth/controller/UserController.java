package com.wts.auth.controller;

import com.wts.auth.dto.StudentImportResult;
import com.wts.auth.dto.BatchIdsDTO;
import com.wts.auth.dto.UserDTO;
import com.wts.auth.entity.SysUser;
import com.wts.auth.service.UserService;
import com.wts.common.exception.BizException;
import com.wts.common.result.PageResult;
import com.wts.common.result.R;
import com.wts.common.security.LoginUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public R<PageResult<SysUser>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String state) {
        PageResult<SysUser> result = userService.listUsers(page, size, keyword, state);
        return R.ok(result);
    }

    @PostMapping
    public R<SysUser> create(@RequestBody UserDTO dto,
                             @AuthenticationPrincipal LoginUserDetails loginUser) {
        SysUser user = userService.createUser(dto, loginUser.getUserId());
        return R.ok(user);
    }

    @PostMapping("/import-students")
    public R<StudentImportResult> importStudents(@RequestParam("file") MultipartFile file,
                                                 @AuthenticationPrincipal LoginUserDetails loginUser) {
        requireTeacher(loginUser);
        if (file == null || file.isEmpty()) {
            throw BizException.fail("请上传学生帐号 Excel 文件");
        }
        try {
            return R.ok(userService.importStudents(file.getInputStream(), loginUser.getUserId()));
        } catch (IOException e) {
            throw BizException.fail("读取上传文件失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public R<SysUser> update(@PathVariable String id, @RequestBody UserDTO dto,
                             @AuthenticationPrincipal LoginUserDetails loginUser) {
        SysUser user = userService.updateUser(id, dto, loginUser.getUserId());
        return R.ok(user);
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id,
                          @AuthenticationPrincipal LoginUserDetails loginUser) {
        userService.deleteUser(id, loginUser.getUserId());
        return R.ok();
    }

    @PostMapping("/batch-disable")
    public R<Void> batchDisable(@RequestBody BatchIdsDTO dto,
                                @AuthenticationPrincipal LoginUserDetails loginUser) {
        userService.disableUsers(dto.getIds(), loginUser.getUserId());
        return R.ok();
    }

    @DeleteMapping("/{id}/hard-delete")
    public R<Void> hardDelete(@PathVariable String id,
                              @AuthenticationPrincipal LoginUserDetails loginUser) {
        userService.hardDeleteUser(id, loginUser.getUserId());
        return R.ok();
    }

    @PostMapping("/batch-hard-delete")
    public R<Void> batchHardDelete(@RequestBody BatchIdsDTO dto,
                                   @AuthenticationPrincipal LoginUserDetails loginUser) {
        userService.hardDeleteUsers(dto.getIds(), loginUser.getUserId());
        return R.ok();
    }

    @PostMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable String id,
                                 @AuthenticationPrincipal LoginUserDetails loginUser) {
        userService.resetPassword(id, loginUser.getUserId());
        return R.ok();
    }

    @PostMapping("/change-password")
    public R<Void> changePassword(@RequestBody java.util.Map<String, String> body,
                                  @AuthenticationPrincipal LoginUserDetails loginUser) {
        userService.changePassword(loginUser.getUserId(), body.get("oldPassword"), body.get("newPassword"));
        return R.ok();
    }

    private void requireTeacher(LoginUserDetails loginUser) {
        String userType = loginUser != null ? loginUser.getUserType() : null;
        if (!"1".equals(userType) && !"3".equals(userType)) {
            throw BizException.forbidden("无权操作");
        }
    }
}
