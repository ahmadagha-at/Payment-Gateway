package at.ahmad.paymentgateway.payment;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("!prod")
@Component("mock")
@Primary
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public boolean processPayment(double amount) {
        System.out.println("[MOCK] Simulating offline payment of \" + amount + \" EUR. No API connection established.");
        return true;
    }
}
