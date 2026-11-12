package com.cybershield.report;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Writes a report as RFC 4180 CSV (UTF-8 with BOM so that Excel shows non-ASCII characters correctly). */
public final class CsvReportWriter {

    private CsvReportWriter() {
    }

    public static byte[] write(ReportData report) {
        StringBuilder sb = new StringBuilder("﻿");
        sb.append(escape(report.title())).append("\r\n");
        sb.append(escape("Generated " + report.generatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + " by " + report.generatedBy())).append("\r\n");
        for (String f : report.filters()) {
            sb.append(escape(f)).append("\r\n");
        }
        sb.append("\r\n");
        appendRow(sb, report.headers());
        for (List<String> row : report.rows()) {
            appendRow(sb, row);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendRow(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(cells.get(i)));
        }
        sb.append("\r\n");
    }

    /** Quotes values and neutralises spreadsheet formulas (CSV injection protection). */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        String v = value;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0 && !v.matches("^-?\\d+(\\.\\d+)?$")) {
            v = "'" + v;
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
