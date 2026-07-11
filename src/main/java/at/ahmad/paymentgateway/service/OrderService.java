package at.ahmad.paymentgateway.service;

import at.ahmad.paymentgateway.model.Order;
import at.ahmad.paymentgateway.model.Product;
import at.ahmad.paymentgateway.model.User;
import at.ahmad.paymentgateway.payment.PaymentProvider;
import at.ahmad.paymentgateway.repository.OrderRepository;
import at.ahmad.paymentgateway.repository.ProductRepository;
import at.ahmad.paymentgateway.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final InvoiceService invoiceService;
    private final EmailService emailService;

    //primary: stripe
    private final PaymentProvider defaultPrimaryProvider;

    private final List<PaymentProvider> allActiveProviders;

    public OrderService(UserRepository userRepo, ProductRepository productRepo,
                        OrderRepository orderRepo, InvoiceService invoiceService,
                        EmailService emailService,
                        PaymentProvider defaultPrimaryProvider,
                        List<PaymentProvider> allActiveProviders) {
        this.userRepo = userRepo;
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.invoiceService = invoiceService;
        this.emailService = emailService;
        this.defaultPrimaryProvider = defaultPrimaryProvider;
        this.allActiveProviders = allActiveProviders;
    }

    @Transactional
    public Order checkout(Long userId, List<Long> productIds, String method) {
        System.out.println("Checkout process initiated for User ID: " + userId);

        // 1. Fetch existing User and Products
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found."));

        List<Product> products = productRepo.findAllById(productIds);
        if (products.isEmpty()) {
            throw new IllegalArgumentException("No valid products found for the given IDs.");
        }

        double totalAmount = products.stream().mapToDouble(Product::getPrice).sum();

        // 2. Initialize Order
        Order order = Order.builder()
                .user(user)
                .products(products)
                .totalAmount(totalAmount)
                .paymentStatus("PENDING")
                .build();
        order = orderRepo.save(order);

        // 3. RESOLVE PAYMENT PROVIDER
        PaymentProvider provider = null;

        // Check if we are in 'dev' profile by looking for our mock provider in the active list
        PaymentProvider mockProvider = allActiveProviders.stream()
                .filter(p -> p.getClass().getSimpleName().equals("MockPaymentProvider"))
                .findFirst()
                .orElse(null);

        if (mockProvider != null) {
            System.out.println("[INFO] 'dev' profile detected. Routing transaction to MockPaymentProvider.");
            provider = mockProvider;
        } else {
            // Prod-Mode active:
            if (method == null || method.trim().isEmpty()) {
                System.out.println("[INFO] No method specified. Using Spring's @Primary bean: " + defaultPrimaryProvider.getClass().getSimpleName());
                provider = defaultPrimaryProvider;
            } else {
                // User requested a specific method
                final String searchedMethod = method.trim().toLowerCase();
                provider = allActiveProviders.stream()
                        .filter(p -> p.getClass().getSimpleName().toLowerCase().startsWith(searchedMethod))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Payment method '" + searchedMethod + "' is not supported."));
                System.out.println("Requested payment method resolved: " + provider.getClass().getSimpleName());
            }
        }

        // 4. Process Payment
        boolean paymentSuccess = provider.processPayment(totalAmount);

        if (paymentSuccess) {
            order.setPaymentStatus("PAID");
            order.setPaymentMethod(provider.getName());
            System.out.println("Payment successful. Order ID " + order.getId() + " updated to PAID.");
            //send email
            byte[] pdfBytes = invoiceService.generateInvoicePdf(order);
            emailService.sendInvoiceEmail(user.getEmail(), pdfBytes);
        } else {
            order.setPaymentStatus("FAILED");
            System.err.println("[WARNING] Payment failed. Order ID " + order.getId() + " updated to FAILED.");
        }

        return orderRepo.save(order);
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if ("PAID".equals(order.getPaymentStatus())) {
            System.out.println("Order " + orderId + " was already paid. Initiating refund...");

            PaymentProvider provider = allActiveProviders.stream()
                    .filter(p -> p.getName().equals(order.getPaymentMethod()))
                    .findFirst()
                    .orElse(defaultPrimaryProvider);

            boolean refundSuccess = provider.refundPayment(order.getTotalAmount());

            if (refundSuccess) {
                order.setPaymentStatus("REFUNDED");
                System.out.println("Refund successful for Order ID: " + orderId);
            } else {
                order.setPaymentStatus("REFUND_FAILED");
                System.err.println("Refund failed for Order ID: " + orderId);
            }
        } else {
            order.setPaymentStatus("CANCELLED");
        }

        return orderRepo.save(order);
    }
}

