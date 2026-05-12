package com.kyvc.backendadmin.domain.report.application;

import com.kyvc.backendadmin.domain.report.dto.AdminOperationReportDtos;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 운영 리포트 PDF 문서를 생성하는 컴포넌트입니다.
 */
@Component
public class OperationReportPdfExporter {

    private static final Path MALGUN_FONT_PATH = Path.of("C:", "Windows", "Fonts", "malgun.ttf");

    /**
     * 운영 리포트 데이터를 PDF 바이트 배열로 변환합니다.
     *
     * @param report 운영 리포트 기본 응답
     * @param kycStatusCounts KYC 상태별 집계
     * @param credentialStatusCounts Credential 상태별 집계
     * @param vpStatusCounts VP 검증 상태별 집계
     * @param aiReviewStatusCounts AI 심사 상태별 집계
     * @param auditActionCounts 감사로그 액션별 집계
     * @return PDF 바이트 배열
     */
    public byte[] export(
            AdminOperationReportDtos.Response report,
            List<AdminOperationReportDtos.StatusCount> kycStatusCounts,
            List<AdminOperationReportDtos.StatusCount> credentialStatusCounts,
            List<AdminOperationReportDtos.StatusCount> vpStatusCounts,
            List<AdminOperationReportDtos.StatusCount> aiReviewStatusCounts,
            List<AdminOperationReportDtos.AuditActionCount> auditActionCounts
    ) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = font(18, Font.BOLD);
            Font sectionFont = font(12, Font.BOLD);
            Font bodyFont = font(9, Font.NORMAL);
            document.add(center("운영 리포트", titleFont));
            document.add(new Paragraph("조회 기간: %s ~ %s".formatted(report.fromDate(), report.toDate()), bodyFont));
            document.add(new Paragraph("생성 일시: %s".formatted(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))), bodyFont));
            document.add(space());
            addSummary(document, report.summary(), sectionFont, bodyFont);
            addStatusTable(document, "KYC 상태별 건수", kycStatusCounts, sectionFont, bodyFont);
            addStatusTable(document, "VC 상태별 건수", credentialStatusCounts, sectionFont, bodyFont);
            addStatusTable(document, "VP 검증 상태별 건수", vpStatusCounts, sectionFont, bodyFont);
            addStatusTable(document, "AI 심사 통계", aiReviewStatusCounts, sectionFont, bodyFont);
            addAuditTable(document, auditActionCounts, sectionFont, bodyFont);
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException exception) {
            throw new IllegalStateException("운영 리포트 PDF 생성에 실패했습니다.", exception);
        }
    }

    private void addSummary(Document document, AdminOperationReportDtos.Summary summary, Font sectionFont, Font bodyFont)
            throws DocumentException {
        document.add(new Paragraph("요약 지표", sectionFont));
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        addHeader(table, bodyFont, "KYC 신청", "KYC 승인", "KYC 반려", "보완요청", "AI 성공", "AI 실패", "수동심사 대기");
        addCells(table, bodyFont,
                summary.kycApplications(),
                summary.kycApproved(),
                summary.kycRejected(),
                summary.supplementRequested(),
                summary.aiReviewSuccess(),
                summary.aiReviewFailed(),
                summary.manualReviewPending());
        document.add(table);
        document.add(space());
    }

    private void addStatusTable(
            Document document,
            String title,
            List<AdminOperationReportDtos.StatusCount> rows,
            Font sectionFont,
            Font bodyFont
    ) throws DocumentException {
        document.add(new Paragraph(title, sectionFont));
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(50);
        addHeader(table, bodyFont, "상태", "건수");
        for (AdminOperationReportDtos.StatusCount row : rows) {
            addCells(table, bodyFont, row.status(), row.count());
        }
        document.add(table);
        document.add(space());
    }

    private void addAuditTable(
            Document document,
            List<AdminOperationReportDtos.AuditActionCount> rows,
            Font sectionFont,
            Font bodyFont
    ) throws DocumentException {
        document.add(new Paragraph("관리자 행위 감사로그 요약", sectionFont));
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        addHeader(table, bodyFont, "대상 유형", "액션", "건수");
        for (AdminOperationReportDtos.AuditActionCount row : rows) {
            addCells(table, bodyFont, row.targetType(), row.action(), row.count());
        }
        document.add(table);
    }

    private Paragraph center(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        return paragraph;
    }

    private Paragraph space() {
        return new Paragraph(" ");
    }

    private void addHeader(PdfPTable table, Font font, String... values) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, font));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private void addCells(PdfPTable table, Font font, Object... values) {
        for (Object value : values) {
            table.addCell(new Phrase(String.valueOf(value), font));
        }
    }

    private Font font(int size, int style) throws IOException, DocumentException {
        if (Files.exists(MALGUN_FONT_PATH)) {
            BaseFont baseFont = BaseFont.createFont(MALGUN_FONT_PATH.toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            return new Font(baseFont, size, style);
        }
        return new Font(Font.HELVETICA, size, style);
    }
}
