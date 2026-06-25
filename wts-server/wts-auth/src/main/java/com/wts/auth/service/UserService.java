package com.wts.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wts.auth.dto.StudentImportResult;
import com.wts.auth.dto.UserDTO;
import com.wts.auth.entity.SysUser;
import com.wts.auth.entity.SysUserorg;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysUserorgMapper userorgMapper;
    private final PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String STUDENT_TYPE = "2";
    private static final String ENABLED_STATE = "1";
    private static final String DISABLED_STATE = "0";
    private static final String SYSADMIN_LOGIN = "sysadmin";
    private static final String DEFAULT_STUDENT_PASSWORD = "123123";

    /**
     * 用户分页列表
     */
    public PageResult<SysUser> listUsers(int page, int size, String keyword, String state) {
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
        wrapper.orderByDesc(SysUser::getCtime);

        Page<SysUser> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        // 隐藏密码
        result.getRecords().forEach(u -> u.setPassword(null));
        return PageResult.of(result);
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

        userMapper.insert(user);
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

        String now = LocalDateTime.now().format(FMT);
        if (StringUtils.hasText(dto.getName())) user.setName(dto.getName());
        if (StringUtils.hasText(dto.getType())) user.setType(dto.getType());
        if (StringUtils.hasText(dto.getState())) user.setState(dto.getState());
        if (dto.getComments() != null) user.setComments(dto.getComments());
        if (StringUtils.hasText(dto.getImgid())) user.setImgid(dto.getImgid());
        user.setUtime(now);
        user.setMuser(operatorId);

        userMapper.updateById(user);
        user.setPassword(null);
        return user;
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
                String comments = getCellText(row, 2, formatter);
                if (!StringUtils.hasText(studentNo) && !StringUtils.hasText(name) && !StringUtils.hasText(comments)) {
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
                    importStudent(studentNo, name, comments, operatorId, result);
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

    private void importStudent(
            String studentNo,
            String name,
            String comments,
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
            user.setState(ENABLED_STATE);
            user.setComments(comments);
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
        existing.setState(ENABLED_STATE);
        existing.setComments(comments);
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
