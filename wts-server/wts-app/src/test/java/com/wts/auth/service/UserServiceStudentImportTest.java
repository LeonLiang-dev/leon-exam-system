package com.wts.auth.service;

import com.wts.auth.dto.StudentImportResult;
import com.wts.auth.entity.SysUser;
import com.wts.auth.mapper.SysUserMapper;
import com.wts.auth.mapper.SysUserorgMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void downloadTemplateProducesWorkbookWithStudentAndGuideSheets() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        service.downloadTemplate(output);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray()))) {
            assertEquals(2, workbook.getNumberOfSheets());
            Sheet studentSheet = workbook.getSheetAt(0);
            assertEquals("学生", studentSheet.getSheetName());
            assertEquals("填写说明", workbook.getSheetAt(1).getSheetName());

            // 表头与 importStudents 解析列一致：学号/姓名/班级
            Row header = studentSheet.getRow(0);
            assertEquals(3, header.getLastCellNum());
            assertEquals("学号", header.getCell(0).getStringCellValue());
            assertEquals("姓名", header.getCell(1).getStringCellValue());
            assertEquals("班级", header.getCell(2).getStringCellValue());
        }
    }

    @Test
    void downloadTemplateGuideSheetContainsInstructions() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        service.downloadTemplate(output);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray()))) {
            Sheet guide = workbook.getSheet("填写说明");
            assertNotNull(guide);
            StringBuilder lines = new StringBuilder();
            for (int i = 0; i <= guide.getLastRowNum(); i++) {
                lines.append(guide.getRow(i).getCell(0).getStringCellValue());
            }
            assertTrue(lines.toString().contains("填写说明"));
            assertTrue(lines.toString().contains("初始密码为 123123"));
            assertTrue(lines.toString().contains("从第2行开始填写"));
        }
    }

    @Test
    void generatedTemplateCanBeFilledThenImported() throws Exception {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("123123")).thenReturn("encoded-123123");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        service.downloadTemplate(output);

        // 在模板「学生」Sheet 第2行补一条数据后按导入流程读取，验证模板表头与解析列一致
        byte[] bytes;
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray()));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("2024099");
            row.createCell(1).setCellValue("模板测试");
            row.createCell(2).setCellValue("软件2401");
            workbook.write(out);
            bytes = out.toByteArray();
        }

        StudentImportResult result = service.importStudents(new ByteArrayInputStream(bytes), "teacher-1");

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getCreated());
        assertEquals(0, result.getFailed());
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(captor.capture());
        SysUser user = captor.getValue();
        assertEquals("2024099", user.getLoginname());
        assertEquals("模板测试", user.getName());
        assertEquals("软件2401", user.getClassName());
        assertEquals("软件2401", user.getComments());
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
