package at.ahmad.paymentgateway.service;

import at.ahmad.paymentgateway.model.Order;
import at.ahmad.paymentgateway.model.Product;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PdfGenerator {

    private final String companyName;
    private final String currency;

    public PdfGenerator(String companyName, String currency) {
        this.companyName = companyName;
        this.currency = currency;
    }

    public byte[] generateInvoice(Order order) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            boolean isRefunded = "REFUNDED".equals(order.getPaymentStatus().toString());
            String documentTitle = isRefunded ? "CANCELLATION CREDIT" : "INVOICE";
            String numberPrefix = isRefunded ? "CN-" : "INV-"; // Credit Note vs Invoice

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(26, 54, 93));
            Font companyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.GRAY);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font whiteBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            // Company Header
            Paragraph compName = new Paragraph(companyName, companyFont);
            compName.setAlignment(Element.ALIGN_LEFT);
            document.add(compName);

            Paragraph title = new Paragraph(documentTitle, titleFont); // Dynamischer Titel
            title.setAlignment(Element.ALIGN_LEFT);
            title.setSpacingAfter(20);
            document.add(title);

            // Info & Customer
            document.add(new Paragraph("Document Number: " + numberPrefix + order.getId(), boldFont));
            document.add(new Paragraph("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm")), normalFont));
            document.add(new Paragraph("Payment Status: " + order.getPaymentStatus(), boldFont));
            document.add(new Paragraph(" ", normalFont)); // spacer

            document.add(new Paragraph("Customer:", boldFont));
            document.add(new Paragraph(order.getUser().getName(), normalFont));
            document.add(new Paragraph(order.getUser().getEmail(), normalFont));
            document.add(new Paragraph(" ", normalFont)); // spacer

            // Create Table of Products
            PdfPTable table = new PdfPTable(2); // 2 columns
            table.setWidthPercentage(100);
            table.setWidths(new float[]{70, 30});
            table.setSpacingBefore(10);
            table.setSpacingAfter(15);

            // Header Cells
            PdfPCell h1 = new PdfPCell(new Phrase("Product", whiteBoldFont));
            h1.setBackgroundColor(new Color(26, 54, 93));
            h1.setPadding(8);
            table.addCell(h1);

            PdfPCell h2 = new PdfPCell(new Phrase("Price", whiteBoldFont));
            h2.setBackgroundColor(new Color(26, 54, 93));
            h2.setPadding(8);
            h2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(h2);

            boolean alternate = false;
            Color altColor = new Color(247, 250, 252); // Very light gray
            for (Product product : order.getProducts()) {
                PdfPCell cellTitle = new PdfPCell(new Phrase(product.getTitle(), normalFont));

                // Preisvorzeichen anpassen, falls es eine Gutschrift ist
                double displayPrice = isRefunded ? -product.getPrice() : product.getPrice();
                PdfPCell cellPrice = new PdfPCell(new Phrase(String.format("%.2f %s", displayPrice, currency), normalFont));
                cellPrice.setHorizontalAlignment(Element.ALIGN_RIGHT);

                cellTitle.setPadding(6);
                cellPrice.setPadding(6);

                if (alternate) {
                    cellTitle.setBackgroundColor(altColor);
                    cellPrice.setBackgroundColor(altColor);
                }
                table.addCell(cellTitle);
                table.addCell(cellPrice);
                alternate = !alternate;
            }

            document.add(table);

            double totalAmount = isRefunded ? -order.getTotalAmount() : order.getTotalAmount();
            String totalLabel = isRefunded ? "Total Refund Amount: " : "Total Amount: ";

            Paragraph total = new Paragraph(
                    String.format("%s %.2f %s", totalLabel, totalAmount, currency),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(26, 54, 93))
            );
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Document creation error during PDF generation", e);
        } catch (Exception e) {
            throw new RuntimeException("Error generating the PDF document", e);
        }
    }
}
