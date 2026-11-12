package com.cybershield.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Renders a report as a landscape A4 PDF with Apache PDFBox 3: title, generation date, filters,
 * a table of database records with a repeated header row, and "Page X of Y" footers.
 */
public final class PdfReportWriter {

    private static final float MARGIN = 36f;
    private static final float ROW_HEIGHT = 16f;
    private static final float FONT_SIZE = 8f;
    private static final float HEADER_FONT_SIZE = 8.5f;

    private PdfReportWriter() {
    }

    public static byte[] write(ReportData report) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDRectangle size = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
            float tableWidth = size.getWidth() - 2 * MARGIN;
            float[] widths = columnWidths(report.columnWeights(), report.headers().size(), tableWidth);

            PDPage page = new PDPage(size);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            float y = size.getHeight() - MARGIN;

            // Title block (first page only)
            cs.setNonStrokingColor(0.06f, 0.16f, 0.33f);
            text(cs, bold, 16, MARGIN, y - 14, clean("CyberShield - " + report.title()));
            cs.setNonStrokingColor(0.3f, 0.3f, 0.3f);
            y -= 32;
            text(cs, regular, 9, MARGIN, y, clean("Generated: " + report.generatedAt()
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) + "   |   By: " + report.generatedBy()
                    + "   |   Records: " + report.rows().size()));
            y -= 13;
            String filters = report.filters().isEmpty() ? "Filters: none (all records)" : "Filters: " + String.join("; ", report.filters());
            for (String line : wrap(clean(filters), regular, 9, tableWidth)) {
                text(cs, regular, 9, MARGIN, y, line);
                y -= 12;
            }
            y -= 6;
            y = drawHeader(cs, bold, report.headers(), widths, y);

            if (report.rows().isEmpty()) {
                cs.setNonStrokingColor(0.3f, 0.3f, 0.3f);
                text(cs, regular, 10, MARGIN + 4, y - 12, "No records match the selected filters.");
            }
            int rowIndex = 0;
            for (List<String> row : report.rows()) {
                if (y - ROW_HEIGHT < MARGIN + 20) {
                    cs.close();
                    page = new PDPage(size);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = size.getHeight() - MARGIN;
                    y = drawHeader(cs, bold, report.headers(), widths, y);
                }
                if (rowIndex % 2 == 1) {
                    cs.setNonStrokingColor(0.95f, 0.96f, 0.98f);
                    cs.addRect(MARGIN, y - ROW_HEIGHT, tableWidth, ROW_HEIGHT);
                    cs.fill();
                }
                cs.setNonStrokingColor(0.1f, 0.1f, 0.1f);
                float x = MARGIN;
                for (int c = 0; c < widths.length; c++) {
                    String value = c < row.size() ? row.get(c) : "";
                    text(cs, regular, FONT_SIZE, x + 3, y - 11, fit(clean(value), regular, FONT_SIZE, widths[c] - 6));
                    x += widths[c];
                }
                y -= ROW_HEIGHT;
                rowIndex++;
            }
            cs.close();

            // Footer with page numbers on every page
            int pages = doc.getNumberOfPages();
            for (int i = 0; i < pages; i++) {
                PDPage p = doc.getPage(i);
                try (PDPageContentStream footer = new PDPageContentStream(doc, p, PDPageContentStream.AppendMode.APPEND,
                        true, true)) {
                    footer.setNonStrokingColor(0.45f, 0.45f, 0.45f);
                    text(footer, regular, 8, MARGIN, MARGIN - 16, "CyberShield - Confidential - internal use only");
                    String label = "Page " + (i + 1) + " of " + pages;
                    float w = regular.getStringWidth(label) / 1000 * 8;
                    text(footer, regular, 8, size.getWidth() - MARGIN - w, MARGIN - 16, label);
                }
            }
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not generate the PDF report", e);
        }
    }

    private static float drawHeader(PDPageContentStream cs, PDType1Font bold, List<String> headers, float[] widths,
                                    float y) throws IOException {
        float total = 0;
        for (float w : widths) {
            total += w;
        }
        cs.setNonStrokingColor(0.06f, 0.16f, 0.33f);
        cs.addRect(MARGIN, y - ROW_HEIGHT - 2, total, ROW_HEIGHT + 2);
        cs.fill();
        cs.setNonStrokingColor(1f, 1f, 1f);
        float x = MARGIN;
        for (int c = 0; c < widths.length; c++) {
            text(cs, bold, HEADER_FONT_SIZE, x + 3, y - 12, fit(clean(headers.get(c)), bold, HEADER_FONT_SIZE, widths[c] - 6));
            x += widths[c];
        }
        return y - ROW_HEIGHT - 2;
    }

    private static void text(PDPageContentStream cs, PDType1Font font, float fontSize, float x, float y, String value)
            throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(value);
        cs.endText();
    }

    private static float[] columnWidths(float[] weights, int columns, float tableWidth) {
        float[] w = new float[columns];
        float sum = 0;
        for (int i = 0; i < columns; i++) {
            w[i] = weights != null && i < weights.length ? weights[i] : 1f;
            sum += w[i];
        }
        for (int i = 0; i < columns; i++) {
            w[i] = w[i] / sum * tableWidth;
        }
        return w;
    }

    /** Shortens text with "..." so it fits in the column. */
    private static String fit(String value, PDType1Font font, float fontSize, float maxWidth) throws IOException {
        if (width(value, font, fontSize) <= maxWidth) {
            return value;
        }
        String v = value;
        while (!v.isEmpty() && width(v + "...", font, fontSize) > maxWidth) {
            v = v.substring(0, v.length() - 1);
        }
        return v + "...";
    }

    private static List<String> wrap(String value, PDType1Font font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (width(candidate, font, fontSize) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static float width(String value, PDType1Font font, float fontSize) throws IOException {
        return font.getStringWidth(value) / 1000 * fontSize;
    }

    /** The standard PDF fonts only support WinAnsi characters; replace anything else. */
    static String clean(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (char ch : value.toCharArray()) {
            if (ch == '–' || ch == '—') {
                sb.append('-');
            } else if (ch == '‘' || ch == '’') {
                sb.append('\'');
            } else if (ch == '“' || ch == '”') {
                sb.append('"');
            } else if (ch == '\n' || ch == '\r' || ch == '\t') {
                sb.append(' ');
            } else if ((ch >= 0x20 && ch <= 0x7E) || (ch >= 0xA0 && ch <= 0xFF)) {
                sb.append(ch);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }
}
