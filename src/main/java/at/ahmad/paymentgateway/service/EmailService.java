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
        System.out.println("Preparing to send email to " + toEmail + "...");
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Your Invoice for Your Order");
            helper.setText("Hello,\n\nThank you for your purchase! Please find your invoice attached to this email as a PDF.\n\nBest regards,\nYour Web Shop Team");

            helper.addAttachment("invoice.pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            System.out.println("Email successfully sent to " + toEmail + ".");
        } catch (Exception e) {
            System.err.println("[WARNING] Could not send email to " + toEmail + ".");
            System.err.println("Details: " + e.getMessage());
        }
    }
}
