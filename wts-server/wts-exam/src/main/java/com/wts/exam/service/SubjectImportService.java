package com.wts.exam.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public interface SubjectImportService {
    /** Import subjects from Excel. Returns import summary. */
    Map<String, Object> importFromExcel(InputStream stream, String typeid, String operatorId, String operatorName);

    /** Export all active subjects to Excel. */
    void exportToExcel(OutputStream stream);

    /** Generate an empty import template (4 sheets with headers). */
    void downloadTemplate(OutputStream stream);
}
