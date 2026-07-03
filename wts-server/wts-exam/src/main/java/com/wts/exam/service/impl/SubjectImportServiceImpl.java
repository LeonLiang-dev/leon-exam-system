package com.wts.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wts.exam.dto.AnswerDTO;
import com.wts.exam.dto.SubjectDTO;
import com.wts.exam.entity.*;
import com.wts.exam.enums.TipType;
import com.wts.exam.mapper.*;
import com.wts.exam.service.SubjectImportService;
import com.wts.exam.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectImportServiceImpl implements SubjectImportService {

    private final SubjectService subjectService;
    private final ExamSubjectMapper subjectMapper;
    private final ExamSubjectVersionMapper versionMapper;
    private final ExamSubjectAnswerMapper answerMapper;
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    @Override
    public Map<String, Object> importFromExcel(InputStream stream, String typeid, String operatorId, String operatorName) {
        int total = 0, success = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(stream)) {
            // Sheet 1: Select/Judge (选择题/判断题)
            if (workbook.getNumberOfSheets() > 0) {
                Sheet sheet = workbook.getSheetAt(0);
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    String type = getStringCell(row, 0);
                    String text = getStringCell(row, 1);
                    if (text == null || text.isBlank()) continue;
                    total++;

                    try {
                        if ("单选".equals(type)) {
                            importSelect(typeid, text, row, "2", operatorId, operatorName);
                        } else if ("多选".equals(type)) {
                            importSelect(typeid, text, row, "3", operatorId, operatorName);
                        }
                        success++;
                    } catch (Exception e) {
                        errors.add("选择题第" + (i + 1) + "行: " + e.getMessage());
                    }
                }
            }

            // Sheet 2: Judge (判断题)
            if (workbook.getNumberOfSheets() > 1) {
                Sheet sheet = workbook.getSheetAt(1);
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    String text = getStringCell(row, 1);
                    if (text == null || text.isBlank()) continue;
                    total++;

                    try {
                        importJudge(typeid, text, row, operatorId, operatorName);
                        success++;
                    } catch (Exception e) {
                        errors.add("判断题第" + (i + 1) + "行: " + e.getMessage());
                    }
                }
            }

            // Sheet 3: Vacancy (填空题)
            if (workbook.getNumberOfSheets() > 2) {
                Sheet sheet = workbook.getSheetAt(2);
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    String text = getStringCell(row, 1);
                    if (text == null || text.isBlank()) continue;
                    total++;

                    try {
                        importVacancy(typeid, text, row, operatorId, operatorName);
                        success++;
                    } catch (Exception e) {
                        errors.add("填空题第" + (i + 1) + "行: " + e.getMessage());
                    }
                }
            }

            // Sheet 4: Interlocution (问答题)
            if (workbook.getNumberOfSheets() > 3) {
                Sheet sheet = workbook.getSheetAt(3);
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    String text = getStringCell(row, 1);
                    if (text == null || text.isBlank()) continue;
                    total++;

                    try {
                        importEssay(typeid, text, "5", row, operatorId, operatorName);
                        success++;
                    } catch (Exception e) {
                        errors.add("问答题第" + (i + 1) + "行: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            throw com.wts.common.exception.BizException.fail("读取Excel文件失败: " + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("errors", errors);
        return result;
    }

    private void importSelect(String typeid, String text, Row row, String tiptype, String operatorId, String operatorName) {
        SubjectDTO dto = new SubjectDTO();
        dto.setTypeid(typeid);
        dto.setTiptype(tiptype);
        dto.setTipstr(text);
        dto.setLevel(getIntCell(row, 9) != null ? getIntCell(row, 9) : 1);
        dto.setPoint(1);

        String rightStr = getStringCell(row, 8);
        List<Integer> rightIndexes = parseRightAnswers(rightStr);

        List<AnswerDTO> answers = new ArrayList<>();
        for (int col = 2; col <= 7; col++) {
            String option = getStringCell(row, col);
            if (option == null || option.isBlank()) continue;
            AnswerDTO ans = new AnswerDTO();
            ans.setAnswer(option);
            ans.setSort(col - 1);
            ans.setRightanswer(rightIndexes.contains(col - 1) ? "1" : "0");
            answers.add(ans);
        }
        dto.setAnswers(answers);
        subjectService.create(dto, operatorId, operatorName);
    }

    private void importJudge(String typeid, String text, Row row, String operatorId, String operatorName) {
        SubjectDTO dto = new SubjectDTO();
        dto.setTypeid(typeid);
        dto.setTiptype("4");
        dto.setTipstr(text);
        dto.setLevel(getIntCell(row, 6) != null ? getIntCell(row, 6) : 1);
        dto.setPoint(1);

        String right = getStringCell(row, 2);
        boolean isCorrect = "对".equals(right != null ? right.trim() : "");

        List<AnswerDTO> answers = new ArrayList<>();
        AnswerDTO correct = new AnswerDTO();
        correct.setAnswer("对");
        correct.setSort(1);
        correct.setRightanswer(isCorrect ? "1" : "0");
        answers.add(correct);

        AnswerDTO wrong = new AnswerDTO();
        wrong.setAnswer("错");
        wrong.setSort(2);
        wrong.setRightanswer(isCorrect ? "0" : "1");
        answers.add(wrong);
        dto.setAnswers(answers);
        subjectService.create(dto, operatorId, operatorName);
    }

    private void importVacancy(String typeid, String text, Row row, String operatorId, String operatorName) {
        SubjectDTO dto = new SubjectDTO();
        dto.setTypeid(typeid);
        dto.setTiptype("1");
        dto.setTipstr(text);
        dto.setLevel(getIntCell(row, 11) != null ? getIntCell(row, 11) : 1);
        dto.setPoint(1);

        List<AnswerDTO> answers = new ArrayList<>();
        for (int col = 2; col <= 7; col++) {
            String answer = getStringCell(row, col);
            if (answer == null || answer.isBlank()) continue;
            AnswerDTO ans = new AnswerDTO();
            ans.setAnswer(answer);
            ans.setSort(col - 1);
            ans.setRightanswer("1");
            answers.add(ans);
        }
        dto.setAnswers(answers);
        subjectService.create(dto, operatorId, operatorName);
    }

    private void importEssay(String typeid, String text, String tiptype, Row row, String operatorId, String operatorName) {
        SubjectDTO dto = new SubjectDTO();
        dto.setTypeid(typeid);
        dto.setTiptype(tiptype);
        dto.setTipstr(text);
        dto.setPoint(1);
        dto.setAnswers(Collections.emptyList());
        subjectService.create(dto, operatorId, operatorName);
    }

    @Override
    public void exportToExcel(OutputStream stream) {
        List<ExamSubject> subjects = subjectMapper.selectList(
                new LambdaQueryWrapper<ExamSubject>().eq(ExamSubject::getPstate, "1"));

        try (Workbook workbook = new XSSFWorkbook()) {
            // Create sheets for each type
            Sheet selectSheet = workbook.createSheet("选择题");
            Sheet judgeSheet = workbook.createSheet("判断题");
            Sheet vacancySheet = workbook.createSheet("填空题");
            Sheet essaySheet = workbook.createSheet("问答题");

            // Headers
            createHeaderRow(selectSheet, workbook, "题型", "题目描述", "选项A", "选项B", "选项C", "选项D", "选项E", "选项F", "答案", "难度");
            createHeaderRow(judgeSheet, workbook, "题型", "题目描述", "答案", "难度");
            createHeaderRow(vacancySheet, workbook, "题型", "题目描述", "空1答案", "空2答案", "空3答案", "空4答案", "空5答案", "空6答案", "难度");
            createHeaderRow(essaySheet, workbook, "题型", "题目描述", "难度");

            int selectRow = 1, judgeRow = 1, vacancyRow = 1, essayRow = 1;

            for (ExamSubject subject : subjects) {
                ExamSubjectVersion version = versionMapper.selectById(subject.getVersionid());
                if (version == null) continue;

                List<ExamSubjectAnswer> answers = answerMapper.selectList(
                        new LambdaQueryWrapper<ExamSubjectAnswer>()
                                .eq(ExamSubjectAnswer::getVersionid, version.getId())
                                .orderByAsc(ExamSubjectAnswer::getSort));

                String tiptype = version.getTiptype();
                TipType tt = TipType.fromCode(tiptype);
                if (tt == null) continue;

                switch (tt) {
                    case SELECT:
                    case CHECKBOX: {
                        Row r = selectSheet.createRow(selectRow++);
                        r.createCell(0).setCellValue(tt == TipType.SELECT ? "单选" : "多选");
                        r.createCell(1).setCellValue(version.getTipstr() != null ? version.getTipstr() : "");
                        StringBuilder right = new StringBuilder();
                        for (int i = 0; i < answers.size() && i < 6; i++) {
                            r.createCell(2 + i).setCellValue(answers.get(i).getAnswer() != null ? answers.get(i).getAnswer() : "");
                            if ("1".equals(answers.get(i).getRightanswer())) {
                                if (right.length() > 0) right.append(",");
                                right.append(i + 1);
                            }
                        }
                        r.createCell(8).setCellValue(right.toString());
                        r.createCell(9).setCellValue(subject.getLevel() != null ? subject.getLevel() : 1);
                        break;
                    }
                    case JUDGE: {
                        Row r = judgeSheet.createRow(judgeRow++);
                        r.createCell(0).setCellValue("判断");
                        r.createCell(1).setCellValue(version.getTipstr() != null ? version.getTipstr() : "");
                        for (ExamSubjectAnswer a : answers) {
                            if ("1".equals(a.getRightanswer())) {
                                r.createCell(2).setCellValue(a.getAnswer() != null ? a.getAnswer() : "");
                                break;
                            }
                        }
                        r.createCell(3).setCellValue(subject.getLevel() != null ? subject.getLevel() : 1);
                        break;
                    }
                    case VACANCY: {
                        Row r = vacancySheet.createRow(vacancyRow++);
                        r.createCell(0).setCellValue("填空");
                        r.createCell(1).setCellValue(version.getTipstr() != null ? version.getTipstr() : "");
                        for (int i = 0; i < answers.size() && i < 6; i++) {
                            r.createCell(2 + i).setCellValue(answers.get(i).getAnswer() != null ? answers.get(i).getAnswer() : "");
                        }
                        r.createCell(8).setCellValue(subject.getLevel() != null ? subject.getLevel() : 1);
                        break;
                    }
                    case INTERLOCUTION: {
                        Row r = essaySheet.createRow(essayRow++);
                        r.createCell(0).setCellValue("问答");
                        r.createCell(1).setCellValue(version.getTipstr() != null ? version.getTipstr() : "");
                        r.createCell(2).setCellValue(subject.getLevel() != null ? subject.getLevel() : 1);
                        break;
                    }
                    case FILEUP:
                        break;
                }
            }

            // Auto-size columns
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet.getRow(0) != null) {
                    for (int j = 0; j < sheet.getRow(0).getLastCellNum(); j++) {
                        sheet.autoSizeColumn(j);
                    }
                }
            }

            workbook.write(stream);
        } catch (IOException e) {
            throw com.wts.common.exception.BizException.fail("导出Excel失败: " + e.getMessage());
        }
    }

    private void createHeaderRow(Sheet sheet, Workbook workbook, String... headers) {
        Row header = sheet.createRow(0);
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private String getStringCell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        String value = DATA_FORMATTER.formatCellValue(cell);
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    private Integer getIntCell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        try {
            return (int) cell.getNumericCellValue();
        } catch (Exception e) {
            try {
                return Integer.parseInt(cell.getStringCellValue());
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private List<Integer> parseRightAnswers(String rightStr) {
        List<Integer> result = new ArrayList<>();
        if (rightStr == null || rightStr.isBlank()) return result;
        for (String s : rightStr.split(",")) {
            try {
                result.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }
}
