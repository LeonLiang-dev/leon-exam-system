package com.wts.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wts.auth.dto.StudentImportResult;
import com.wts.auth.dto.UserDTO;
import com.wts.auth.entity.SysOrganization;
import com.wts.auth.entity.SysUser;
import com.wts.auth.entity.SysUserorg;
import com.wts.auth.enums.UserPost;
import com.wts.auth.mapper.SysOrganizationMapper;
import com.wts.auth.mapper.SysUserMapper;
import com.wts.auth.mapper.SysUserorgMapper;
import com.wts.common.exception.BizException;
import com.wts.common.result.PageResult;
import com.wts.common.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysUserorgMapper userorgMapper;
    private final SysOrganizationMapper organizationMapper;
    private final PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String STUDENT_TYPE = "2";
    private static final String ENABLED_STATE = "1";
    private static final String DISABLED_STATE = "0";
    private static final String SYSADMIN_LOGIN = "sysadmin";
    private static final String DEFAULT_STUDENT_PASSWORD = "123123";

    /**
     * 学生分页列表
     *
     * @param scopeUserIds 可见用户 id 集合；null 表示不限制（学生全院共用时传入）
     */
    public PageResult<SysUser> listUsers(int page, int size, String keyword, String state,
                                         String post, String className, String orgId, List<String> scopeUserIds) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(SysUser::getName, keyword)
                    .or().like(SysUser::getLoginname, keyword)
            );
        }
        if (StringUtils.hasText(state)) {
            wrapper.eq(SysUser::getState, state);
        }
        if (StringUtils.hasText(post)) {
            // 支持逗号分隔多职位（如 teacher,director,deputy → in）
            String[] posts = post.split(",");
            if (posts.length == 1) {
                wrapper.eq(SysUser::getPost, posts[0].trim());
            } else {
                wrapper.in(SysUser::getPost, java.util.Arrays.stream(posts)
                        .map(String::trim).filter(StringUtils::hasText).toList());
            }
        }
        if (StringUtils.hasText(className)) {
            wrapper.like(SysUser::getClassName, className);
        }
        if (StringUtils.hasText(orgId)) {
            Set<String> orgScope = expandOrgSubtree(orgId);
            if (orgScope.isEmpty()) {
                wrapper.eq(SysUser::getId, "__NONE__");
            } else {
                Set<String> orgUserIds = userorgMapper.selectList(
                                new LambdaQueryWrapper<SysUserorg>()
                                        .in(SysUserorg::getOrganizationid, orgScope))
                        .stream()
                        .map(SysUserorg::getUserid)
                        .collect(Collectors.toSet());
                if (orgUserIds.isEmpty()) {
                    wrapper.eq(SysUser::getId, "__NONE__");
                } else {
                    wrapper.in(SysUser::getId, orgUserIds);
                }
            }
        }
        if (scopeUserIds != null) {
            if (scopeUserIds.isEmpty()) {
                wrapper.eq(SysUser::getId, "__NONE__");
            } else {
                wrapper.in(SysUser::getId, scopeUserIds);
            }
        }
        wrapper.orderByDesc(SysUser::getCtime);

        Page<SysUser> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        // 隐藏密码
        result.getRecords().forEach(u -> u.setPassword(null));
        fillOrgInfo(result.getRecords());
        return PageResult.of(result);
    }

    /** 按 id 批量读取用户（供权限判定使用，不做任何写保护检查） */
    public List<SysUser> listByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userMapper.selectBatchIds(ids);
    }

    /** 批量填充用户组织归属 id 与名称（一次 IN 查询） */
    private void fillOrgInfo(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<String> userIds = users.stream().map(SysUser::getId).distinct().collect(Collectors.toList());
        List<SysUserorg> userOrgs = userorgMapper.selectList(
                new LambdaQueryWrapper<SysUserorg>().in(SysUserorg::getUserid, userIds));
        Map<String, String> orgByUser = userOrgs.stream()
                .collect(Collectors.toMap(SysUserorg::getUserid, SysUserorg::getOrganizationid, (a, b) -> a));
        if (orgByUser.isEmpty()) {
            return;
        }
        Map<String, String> nameByOrg = organizationMapper.selectList(
                        new LambdaQueryWrapper<SysOrganization>()
                                .in(SysOrganization::getId, new HashSet<>(orgByUser.values())))
                .stream()
                .collect(Collectors.toMap(SysOrganization::getId, SysOrganization::getName, (a, b) -> a));
        users.forEach(u -> {
            String orgId = orgByUser.get(u.getId());
            u.setOrgId(orgId);
            u.setOrgName(orgId != null ? nameByOrg.get(orgId) : null);
        });
    }

    /** 展开组织节点及其全部子孙节点 id 集合 */
    private Set<String> expandOrgSubtree(String orgId) {
        Set<String> result = new HashSet<>();
        List<SysOrganization> all = organizationMapper.selectList(null);
        if (all.isEmpty()) {
            return result;
        }
        Map<String, List<SysOrganization>> childrenByParent = new HashMap<>();
        for (SysOrganization org : all) {
            childrenByParent.computeIfAbsent(org.getParentid(), k -> new ArrayList<>()).add(org);
        }
        collectSubtree(orgId, childrenByParent, result);
        return result;
    }

    private void collectSubtree(String nodeId, Map<String, List<SysOrganization>> childrenByParent, Set<String> out) {
        if (nodeId == null || !out.add(nodeId)) {
            return;
        }
        for (SysOrganization child : childrenByParent.getOrDefault(nodeId, List.of())) {
            collectSubtree(child.getId(), childrenByParent, out);
        }
    }

    /**
     * 创建用户
     */
    @Transactional
    public SysUser createUser(UserDTO dto, String operatorId) {
        // 检查登录名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getLoginname, dto.getLoginname())
        );
        if (count > 0) {
            throw BizException.fail("登录名已存在: " + dto.getLoginname());
        }

        String now = LocalDateTime.now().format(FMT);
        SysUser user = new SysUser();
        user.setId(UUID.randomUUID().toString().replace("-", ""));
        user.setName(dto.getName());
        user.setLoginname(dto.getLoginname());
        // 新用户默认密码: 123456，BCrypt 加密
        user.setPassword(passwordEncoder.encode("123456"));
        user.setType(dto.getType() != null ? dto.getType() : "1");
        user.setState(dto.getState() != null ? dto.getState() : "1");
        user.setComments(dto.getComments());
        user.setCtime(now);
        user.setUtime(now);
        user.setCuser(operatorId);
        user.setMuser(operatorId);
        user.setUuid(user.getId());

        applyIdentityFields(user, dto, now);
        userMapper.insert(user);

        bindOrganization(user.getId(), dto.getOrgId());
        user.setPassword(null);
        return user;
    }

    /**
     * 更新用户
     */
    @Transactional
    public SysUser updateUser(String id, UserDTO dto, String operatorId) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }

        // 内置超级管理员的职位/权限不可修改（防止锁死或自降级）
        if (SYSADMIN_LOGIN.equalsIgnoreCase(user.getLoginname())
                && (StringUtils.hasText(dto.getPost()) || dto.getPerms() != null)) {
            throw BizException.fail("系统管理员不允许修改职位与权限");
        }
        // 任何人不能修改自己的职位/权限（防止自升/自降级造成权限失控）
        if (Objects.equals(id, operatorId)
                && (StringUtils.hasText(dto.getPost()) || dto.getPerms() != null)) {
            throw BizException.fail("不能修改自己的职位与权限");
        }

        String now = LocalDateTime.now().format(FMT);
        if (StringUtils.hasText(dto.getName())) user.setName(dto.getName());
        if (StringUtils.hasText(dto.getType())) user.setType(dto.getType());
        if (StringUtils.hasText(dto.getState())) user.setState(dto.getState());
        if (dto.getComments() != null) user.setComments(dto.getComments());
        if (StringUtils.hasText(dto.getImgid())) user.setImgid(dto.getImgid());
        if (dto.getClassName() != null) user.setClassName(dto.getClassName());
        if (StringUtils.hasText(dto.getPost())) {
            user.setPost(dto.getPost());
            user.setPerms(dto.getPerms() != null ? dto.getPerms() : String.join(",", PermissionService.defaultPerms(dto.getPost())));
        }
        user.setUtime(now);
        user.setMuser(operatorId);

        userMapper.updateById(user);

        if (dto.getOrgId() != null) {
            bindOrganization(id, dto.getOrgId());
        }
        user.setPassword(null);
        return user;
    }

    /** 设置职位/权限/班级（职位未显式指定时由 type 推导） */
    private void applyIdentityFields(SysUser user, UserDTO dto, String now) {
        String post;
        if (StringUtils.hasText(dto.getPost())) {
            post = dto.getPost();
        } else {
            post = "2".equals(user.getType()) ? UserPost.STUDENT.code() : UserPost.TEACHER.code();
        }
        user.setPost(post);
        user.setPerms(StringUtils.hasText(dto.getPerms())
                ? dto.getPerms()
                : String.join(",", PermissionService.defaultPerms(post)));
        user.setClassName(dto.getClassName());
    }

    /** 维护用户-组织归属（先清后插，保证唯一归属） */
    private void bindOrganization(String userId, String orgId) {
        if (!StringUtils.hasText(orgId)) {
            return;
        }
        userorgMapper.delete(new LambdaQueryWrapper<SysUserorg>().eq(SysUserorg::getUserid, userId));
        SysUserorg userorg = new SysUserorg();
        userorg.setId(UUID.randomUUID().toString().replace("-", ""));
        userorg.setUserid(userId);
        userorg.setOrganizationid(orgId);
        userorgMapper.insert(userorg);
    }

    /**
     * 重置用户密码
     */
    public void resetPassword(String id, String operatorId) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        String now = LocalDateTime.now().format(FMT);
        user.setPassword(passwordEncoder.encode("123456"));
        user.setUtime(now);
        user.setMuser(operatorId);
        userMapper.updateById(user);
    }

    /**
     * 修改密码
     */
    public void changePassword(String userId, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw BizException.fail("密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw BizException.fail("新密码长度不能少于6位");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }

        // 验证旧密码
        boolean match = false;
        if (PasswordUtils.isBCrypt(user.getPassword())) {
            match = passwordEncoder.matches(oldPassword, user.getPassword());
        } else if (PasswordUtils.isMd5(user.getPassword())) {
            match = PasswordUtils.md5Password(oldPassword, user.getLoginname()).equalsIgnoreCase(user.getPassword());
        }

        if (!match) {
            throw BizException.fail("旧密码错误");
        }

        String now = LocalDateTime.now().format(FMT);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUtime(now);
        user.setMuser(userId);
        userMapper.updateById(user);
    }

    /**
     * 删除用户 (逻辑删除: 设为禁用)
     */
    public void deleteUser(String id, String operatorId) {
        disableUsers(List.of(id), operatorId);
    }

    /**
     * 批量禁用用户。
     */
    @Transactional
    public void disableUsers(List<String> ids, String operatorId) {
        List<SysUser> users = loadUsersForWrite(ids, operatorId, "禁用");
        String now = LocalDateTime.now().format(FMT);
        for (SysUser user : users) {
            user.setState(DISABLED_STATE);
            user.setUtime(now);
            user.setMuser(operatorId);
            userMapper.updateById(user);
        }
    }

    /**
     * 永久删除单个用户。
     */
    public void hardDeleteUser(String id, String operatorId) {
        hardDeleteUsers(List.of(id), operatorId);
    }

    /**
     * 批量永久删除用户。
     */
    @Transactional
    public void hardDeleteUsers(List<String> ids, String operatorId) {
        List<SysUser> users = loadUsersForWrite(ids, operatorId, "删除");
        List<String> userIds = users.stream().map(SysUser::getId).toList();
        userorgMapper.delete(new LambdaQueryWrapper<SysUserorg>().in(SysUserorg::getUserid, userIds));
        userMapper.deleteBatchIds(userIds);
    }

    private List<SysUser> loadUsersForWrite(List<String> ids, String operatorId, String actionName) {
        if (ids == null || ids.isEmpty()) {
            throw BizException.fail("请选择要" + actionName + "的用户");
        }
        List<String> normalizedIds = ids.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (normalizedIds.isEmpty()) {
            throw BizException.fail("请选择要" + actionName + "的用户");
        }

        List<SysUser> users = userMapper.selectBatchIds(normalizedIds);
        Set<String> foundIds = users.stream().map(SysUser::getId).collect(Collectors.toSet());
        List<String> missingIds = normalizedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw BizException.fail("部分用户不存在: " + String.join(", ", missingIds));
        }

        for (SysUser user : users) {
            if (Objects.equals(user.getId(), operatorId)) {
                throw BizException.fail("不能" + actionName + "当前登录用户");
            }
            if (SYSADMIN_LOGIN.equalsIgnoreCase(user.getLoginname())) {
                throw BizException.fail("不能" + actionName + "内置超级管理员 sysadmin");
            }
        }
        return users;
    }

    @Transactional
    public StudentImportResult importStudents(InputStream stream, String operatorId) {
        StudentImportResult result = new StudentImportResult();
        DataFormatter formatter = new DataFormatter();
        Set<String> importedStudentNos = new HashSet<>();

        try (Workbook workbook = WorkbookFactory.create(stream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw BizException.fail("Excel 文件没有工作表");
            }

            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String studentNo = getCellText(row, 0, formatter);
                String name = getCellText(row, 1, formatter);
                String className = getCellText(row, 2, formatter);
                if (!StringUtils.hasText(studentNo) && !StringUtils.hasText(name) && !StringUtils.hasText(className)) {
                    continue;
                }

                int rowNum = i + 1;
                if (!StringUtils.hasText(studentNo)) {
                    result.addError("第" + rowNum + "行: 学号不能为空");
                    continue;
                }
                if (!StringUtils.hasText(name)) {
                    result.addError("第" + rowNum + "行: 姓名不能为空");
                    continue;
                }
                if (!importedStudentNos.add(studentNo)) {
                    result.addError("第" + rowNum + "行: 学号重复 " + studentNo);
                    continue;
                }

                try {
                    importStudent(studentNo, name, className, operatorId, result);
                } catch (Exception e) {
                    result.addError("第" + rowNum + "行: " + e.getMessage());
                }
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw BizException.fail("读取 Excel 文件失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 生成学生导入模板：表头与导入解析列完全一致（学号/姓名/班级）。
     */
    public void downloadTemplate(OutputStream stream) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet studentSheet = workbook.createSheet("学生");
            Sheet guideSheet = workbook.createSheet("填写说明");

            // 表头与 importStudents 解析列一致：第0列学号、第1列姓名、第2列班级
            Row header = studentSheet.createRow(0);
            String[] headers = {"学号", "姓名", "班级"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            studentSheet.setColumnWidth(0, 20 * 256);
            studentSheet.setColumnWidth(1, 20 * 256);
            studentSheet.setColumnWidth(2, 24 * 256);

            // 填写说明放独立 Sheet，导入仅读取第 1 个 Sheet，不会误导入
            String[] guideLines = {
                    "模板填写说明",
                    "1.【学号】学生登录账号，必填，不能重复",
                    "2.【姓名】必填",
                    "3.【班级】选填，如 软件2401 班",
                    "4. 从第2行开始填写；初始密码为 123123",
            };
            for (int i = 0; i < guideLines.length; i++) {
                guideSheet.createRow(i).createCell(0).setCellValue(guideLines[i]);
            }
            guideSheet.setColumnWidth(0, 60 * 256);

            workbook.write(stream);
        } catch (java.io.IOException e) {
            throw BizException.fail("生成模板失败: " + e.getMessage());
        }
    }

    private void importStudent(
            String studentNo,
            String name,
            String className,
            String operatorId,
            StudentImportResult result) {
        String now = LocalDateTime.now().format(FMT);
        SysUser existing = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getLoginname, studentNo));

        if (existing == null) {
            SysUser user = new SysUser();
            user.setId(UUID.randomUUID().toString().replace("-", ""));
            user.setName(name);
            user.setLoginname(studentNo);
            user.setPassword(passwordEncoder.encode(DEFAULT_STUDENT_PASSWORD));
            user.setType(STUDENT_TYPE);
            user.setPost(UserPost.STUDENT.code());
            user.setState(ENABLED_STATE);
            user.setComments(className);
            user.setClassName(className);
            user.setCtime(now);
            user.setUtime(now);
            user.setCuser(operatorId);
            user.setMuser(operatorId);
            user.setUuid(user.getId());
            userMapper.insert(user);
            result.addCreated();
            return;
        }

        existing.setName(name);
        existing.setType(STUDENT_TYPE);
        existing.setPost(UserPost.STUDENT.code());
        existing.setState(ENABLED_STATE);
        existing.setComments(className);
        existing.setClassName(className);
        existing.setUtime(now);
        existing.setMuser(operatorId);
        userMapper.updateById(existing);
        result.addUpdated();
    }

    private String getCellText(Row row, int index, DataFormatter formatter) {
        String value = formatter.formatCellValue(row.getCell(index));
        return value != null ? value.trim() : null;
    }
}
