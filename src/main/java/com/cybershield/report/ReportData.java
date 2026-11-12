package com.cybershield.report;

import java.time.LocalDateTime;
import java.util.List;

/** A generated report: title, applied filters, column headers, relative column widths and the data rows. */
public record ReportData(String title, LocalDateTime generatedAt, String generatedBy, List<String> filters,
                         List<String> headers, float[] columnWeights, List<List<String>> rows) {
}
