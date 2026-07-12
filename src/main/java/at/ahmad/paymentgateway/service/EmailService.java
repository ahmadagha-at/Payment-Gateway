package at.ahmad.paymentgateway.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendInvoiceEmail(String toEmail, byte[] pdfBytes) {
        System.out.println("Preparing to send checkout invoice email to " + toEmail + "...");
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Your Invoice for Your Order");
            helper.setText("Hello,\n\nThank you for your purchase! Please find your invoice attached to this email as a PDF.\n\nBest regards,\nYour Web Shop Team");

            helper.addAttachment("invoice.pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            System.out.println("Invoice email successfully sent to " + toEmail + ".");
        } catch (Exception e) {
            System.err.println("[WARNING] Could not send invoice email to " + toEmail + ".");
            System.err.println("Details: " + e.getMessage());
        }
    }

    public void sendCancellationEmail(String toEmail, byte[] pdfBytes) {
        System.out.println("Preparing to send order cancellation email to " + toEmail + "...");
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Your Order Has Been Cancelled & will be Refunded");
            helper.setText("Hello,\n\nYour order has been successfully cancelled and your payment will be refunded. Please find your cancellation credit note attached to this email as a PDF.\n\nBest regards,\nYour Web Shop Team");

            helper.addAttachment("cancellation_credit.pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            System.out.println("Cancellation email successfully sent to " + toEmail + ".");
        } catch (Exception e) {
            System.err.println("[WARNING] Could not send cancellation email to " + toEmail + ".");
            System.err.println("Details: " + e.getMessage());
        }
    }
}
