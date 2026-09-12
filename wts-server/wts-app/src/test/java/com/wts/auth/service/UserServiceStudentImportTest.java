package com.wts.auth.service;

import com.wts.auth.dto.StudentImportResult;
import com.wts.auth.entity.SysUser;
import com.wts.auth.mapper.SysUserMapper;
import com.wts.auth.mapper.SysUserorgMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceStudentImportTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysUserorgMapper userorgMapper;
    @Mock
    private com.wts.auth.mapper.SysOrganizationMapper organizationMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userMapper, userorgMapper, organizationMapper, passwordEncoder);
    }

    @Test
    void importStudentsCreatesStudentWithStudentNoAndDefaultPassword() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("123123")).thenReturn("encoded-123123");

        StudentImportResult result = service.importStudents(
                workbook(
                        row("学号", "姓名", "备注"),
                        row("2024001", "张三", "一班")
                ),
                "teacher-1"
        );

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getCreated());
        assertEquals(0, result.getUpdated());
        assertEquals(0, result.getFailed());

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(userCaptor.capture());
        SysUser user = userCaptor.getValue();
        assertNotNull(user.getId());
        assertEquals("张三", user.getName());
        assertEquals("2024001", user.getLoginname());
        assertEquals("encoded-123123", user.getPassword());
        assertEquals("2", user.getType());
        assertEquals("1", user.getState());
        assertEquals("一班", user.getComments());
        assertEquals("teacher-1", user.getCuser());
        assertEquals("teacher-1", user.getMuser());
    }

    @Test
    void importStudentsUpdatesExistingStudentWithoutResettingPassword() {
        SysUser existing = new SysUser();
        existing.setId("student-1");
        existing.setLoginname("2024001");
        existing.setPassword("existing-password");
        existing.setType("2");
        existing.setState("0");
        when(userMapper.selectOne(any())).thenReturn(existing);

        StudentImportResult result = service.importStudents(
                workbook(
                        row("学号", "姓名", "备注"),
                        row("2024001", "李四", "二班")
                ),
                "teacher-1"
        );

        assertEquals(1, result.getTotal());
        assertEquals(0, result.getCreated());
        assertEquals(1, result.getUpdated());
        assertEquals(0, result.getFailed());
        assertEquals("李四", existing.getName());
        assertEquals("existing-password", existing.getPassword());
        assertEquals("2", existing.getType());
        assertEquals("1", existing.getState());
        assertEquals("二班", existing.getComments());
        assertEquals("teacher-1", existing.getMuser());
        verify(passwordEncoder, never()).encode(any());
        verify(userMapper).updateById(existing);
    }

    @Test
    void importStudentsReportsInvalidRowsAndDuplicateStudentNo() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("123123")).thenReturn("encoded-123123");

        StudentImportResult result = service.importStudents(
                workbook(
                        row("学号", "姓名", "备注"),
                        row("", "无学号", ""),
                        row("2024001", "", ""),
                        row("2024002", "王五", ""),
                        row("2024002", "重复", "")
                ),
                "teacher-1"
        );

        assertEquals(4, result.getTotal());
        assertEquals(1, result.getCreated());
        assertEquals(0, result.getUpdated());
        assertEquals(3, result.getFailed());
        assertEquals(3, result.getErrors().size());
        verify(userMapper).insert(any(SysUser.class));
    }

    private static InputStream workbook(String[]... rows) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("学生帐号");
            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i);
                for (int j = 0; j < rows[i].length; j++) {
                    row.createCell(j).setCellValue(rows[i][j]);
                }
            }
            workbook.write(output);
            return new ByteArrayInputStream(output.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String[] row(String studentNo, String name, String comments) {
        return new String[]{studentNo, name, comments};
    }
}
