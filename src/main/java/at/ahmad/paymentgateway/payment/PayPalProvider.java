package at.ahmad.paymentgateway.payment;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("prod")
@Component("paypal")
public class PayPalProvider implements PaymentProvider {

    @Override
    public boolean processPayment(double amount) {
        System.out.println("Connecting to PayPal...");
        System.out.println("Payment of " + amount + " EUR successful.");
        return true;
    }
}
