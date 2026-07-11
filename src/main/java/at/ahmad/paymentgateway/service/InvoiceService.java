package at.ahmad.paymentgateway.service;

import at.ahmad.paymentgateway.model.Order;
import org.springframework.stereotype.Service;

@Service
public class InvoiceService {

    private final PdfGenerator pdfGenerator;

    public InvoiceService(PdfGenerator pdfGenerator) {
        this.pdfGenerator = pdfGenerator;
    }

    public byte[] generateInvoicePdf(Order order) {
        System.out.println("Generating PDF invoice for order ID: " + order.getId());
        return pdfGenerator.generateInvoice(order);
    }
}
