package at.ahmad.paymentgateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InvoiceConfig {

    //values in application.yaml
    @Value("${shop.billing.company-name:MyShop}")
    private String companyName;

    @Value("${shop.billing.currency:EUR}")
    private String currency;

    @Bean
    public PdfGenerator pdfGenerator() {
        return new PdfGenerator(companyName, currency);
    }
}
