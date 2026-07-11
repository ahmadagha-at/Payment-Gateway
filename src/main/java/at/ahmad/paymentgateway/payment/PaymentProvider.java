package at.ahmad.paymentgateway.payment;

public interface PaymentProvider {
    boolean processPayment(double amount);

    default String getName(){
        return this.getClass().getSimpleName();
    }

    default boolean refundPayment(double amount){
        System.out.println("[MOCK] Refunding " + amount + " EUR to user.");
        return true;
    }
}
