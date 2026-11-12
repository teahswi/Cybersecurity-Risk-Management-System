package com.cybershield.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.cybershield.report.CsvReportWriter;
import com.cybershield.report.PdfReportWriter;
import com.cybershield.report.ReportData;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class ReportWriterTest {

    private static ReportData sample(int rows) {
        List<List<String>> data = new ArrayList<>();
        for (int i = 1; i <= rows; i++) {
            data.add(List.of("SRV-" + i, "Server, \"quoted\" " + i, "=SUM(A1)"));
        }
        return new ReportData("Asset inventory report", LocalDateTime.now(), "Test User", List.of("Status = Active"),
                List.of("Code", "Name", "Formula"), new float[]{1, 3, 1}, data);
    }

    @Test
    void csvEscapesQuotesCommasAndFormulas() {
        String csv = new String(CsvReportWriter.write(sample(2)), StandardCharsets.UTF_8);
        assertThat(csv).contains("Code,Name,Formula");
        assertThat(csv).contains("\"Server, \"\"quoted\"\" 1\"");
        assertThat(csv).contains("'=SUM(A1)");
    }

    @Test
    void pdfContainsTitleRecordsAndPageNumbers() throws Exception {
        byte[] pdf = PdfReportWriter.write(sample(120));
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Asset inventory report").contains("SRV-120").contains("Page 1 of");
        }
    }
}
