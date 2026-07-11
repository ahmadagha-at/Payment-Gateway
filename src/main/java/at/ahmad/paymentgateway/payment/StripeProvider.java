package at.ahmad.paymentgateway.payment;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;


@Profile("prod")
@Primary
@Component("stripe")
public class StripeProvider implements PaymentProvider {

    @Override
    public boolean processPayment(double amount) {
        System.out.println("Connecting to Stripe Provider...");
        System.out.println("Payment of " + amount + " EUR successful.");
        return true;
    }
}
