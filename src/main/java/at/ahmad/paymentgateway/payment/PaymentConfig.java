package at.ahmad.paymentgateway.payment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * ALTERNATIVE CONFIGURATION APPROACH:
 * (putting @Component and @Profile directly on the provider classes).
 * * IF YOU WANT TO USE THIS CONFIGURATION CLASS INSTEAD:
 * 1. Remove the comments from this class and add the @Configuration annotation.
 * 2. Go to the individual provider classes (PayPalProvider, StripeProvider, MockPaymentProvider).
 * 3. REMOVE the @Component and @Profile annotations from those classes completely.
 * * Why use this? It keeps the provider classes "clean" and centralizes
 * all bean creation and profile switching in one single place.
 */
/* @Configuration
public class PaymentConfig {

    @Bean("paypal")
    @Profile("prod")
    public PaymentProvider payPalProvider() {
        return new PayPalProvider();
    }

    @Bean("stripe")
    @Profile("prod")
    public PaymentProvider stripeProvider() {
        return new StripeProvider();
    }

    @Bean("testPaymentProvider")
    @Profile("dev")
    public PaymentProvider testPaymentProvider() {
        return new MockPaymentProvider();
    }
}
*/
