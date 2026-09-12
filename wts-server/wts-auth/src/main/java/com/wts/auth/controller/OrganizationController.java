package com.wts.auth.controller;

import com.wts.auth.entity.SysOrganization;
import com.wts.auth.service.OrganizationService;
import com.wts.auth.service.PermissionService;
import com.wts.common.result.R;
import com.wts.common.security.CurrentUser;
import com.wts.common.security.CurrentUserProvider;
import com.wts.common.security.LoginUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * 组织树按当前用户可见范围返回：
     * 平台管理员 = 全部；主任/副主任 = 本教研室子树；其他 = 空。
     */
    @GetMapping("/tree")
    public R<List<OrganizationService.OrgTreeNode>> getTree() {
        CurrentUser user = currentUserProvider.require();
        return R.ok(organizationService.getOrgTree(permissionService.visibleOrgIds(user)));
    }

    @PostMapping
    public R<SysOrganization> create(@RequestBody SysOrganization org,
                                     @AuthenticationPrincipal LoginUserDetails loginUser) {
        return R.ok(organizationService.createOrganization(org, loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    public R<SysOrganization> update(@PathVariable String id,
                                     @RequestBody SysOrganization org,
                                     @AuthenticationPrincipal LoginUserDetails loginUser) {
        return R.ok(organizationService.updateOrganization(id, org, loginUser.getUserId()));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id,
                          @AuthenticationPrincipal LoginUserDetails loginUser) {
        organizationService.deleteOrganization(id, loginUser.getUserId());
        return R.ok();
    }
}
