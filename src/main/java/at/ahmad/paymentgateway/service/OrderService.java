package at.ahmad.paymentgateway.service;

import at.ahmad.paymentgateway.model.Order;
import at.ahmad.paymentgateway.model.Product;
import at.ahmad.paymentgateway.model.User;
import at.ahmad.paymentgateway.payment.PaymentProvider;
import at.ahmad.paymentgateway.repository.OrderRepository;
import at.ahmad.paymentgateway.repository.ProductRepository;
import at.ahmad.paymentgateway.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;

import at.ahmad.paymentgateway.transactionalEventListeners.CancelOrderEvent;
import at.ahmad.paymentgateway.transactionalEventListeners.CheckoutEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class OrderService {

    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final ApplicationEventPublisher eventPublisher;

    // Primary provider, zum Beispiel Stripe
    private final PaymentProvider defaultPrimaryProvider;
    private final List<PaymentProvider> allActiveProviders;

    public OrderService(
            UserRepository userRepo,
            ProductRepository productRepo,
            OrderRepository orderRepo,
            ApplicationEventPublisher eventPublisher,
            PaymentProvider defaultPrimaryProvider,
            List<PaymentProvider> allActiveProviders
    ) {
        this.userRepo = userRepo;
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.eventPublisher = eventPublisher;
        this.defaultPrimaryProvider = defaultPrimaryProvider;
        this.allActiveProviders = allActiveProviders;
    }

    @Transactional
    public Order checkout(
            Long userId,
            List<Long> productIds,
            String method
    ) {
        System.out.println(
                "Checkout process initiated for User ID: " + userId
        );

        // 1. User laden
        User user = userRepo.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User with ID " + userId + " not found."
                        )
                );

        // 2. Produkte laden
        List<Product> products = productRepo.findAllById(productIds);

        if (products.isEmpty()) {
            throw new IllegalArgumentException(
                    "No valid products found for the given IDs."
            );
        }

        double totalAmount = products.stream()
                .mapToDouble(Product::getPrice)
                .sum();

        // 3. Order zunächst mit Status PENDING speichern
        Order order = Order.builder()
                .user(user)
                .products(products)
                .totalAmount(totalAmount)
                .paymentStatus("PENDING")
                .build();

        order = orderRepo.save(order);

        // 4. PaymentProvider bestimmen
        PaymentProvider provider;

        PaymentProvider mockProvider = allActiveProviders.stream()
                .filter(p ->
                        "MockPaymentProvider".equals(p.getName())
                )
                .findFirst()
                .orElse(null);

        if (mockProvider != null) {
            System.out.println(
                    "[INFO] 'dev' profile detected. "
                            + "Routing transaction to MockPaymentProvider."
            );

            provider = mockProvider;

        } else if (method == null || method.trim().isEmpty()) {

            System.out.println(
                    "[INFO] No method specified. Using Spring's "
                            + "@Primary bean: "
                            + defaultPrimaryProvider.getName()
            );

            provider = defaultPrimaryProvider;

        } else {

            String searchedMethod = method.trim().toLowerCase();

            provider = allActiveProviders.stream()
                    .filter(p ->
                            p.getName()
                                    .toLowerCase()
                                    .contains(searchedMethod)
                    )
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Payment method '"
                                            + searchedMethod
                                            + "' is not supported."
                            )
                    );

            System.out.println(
                    "Requested payment method resolved: "
                            + provider.getName()
            );
        }

        // 5. Zahlung durchführen
        boolean paymentSuccess =
                provider.processPayment(totalAmount);

        if (paymentSuccess) {

            order.setPaymentStatus("PAID");
            order.setPaymentMethod(provider.getName());

            System.out.println(
                    "Payment successful. Order ID "
                            + order.getId()
                            + " updated to PAID."
            );

            order = orderRepo.save(order);

            /*
             * Der OrderService sendet keine E-Mail direkt.
             * Er veröffentlicht nur das Event.
             *
             * Der OrderEventListener sendet die E-Mail
             * nach dem erfolgreichen Commit.
             */
            eventPublisher.publishEvent(
                    new CheckoutEvent(order.getId())
            );

        } else {

            order.setPaymentStatus("FAILED");

            System.err.println(
                    "[WARNING] Payment failed. Order ID "
                            + order.getId()
                            + " updated to FAILED."
            );

            order = orderRepo.save(order);
        }

        return order;
    }

    @Transactional
    public Order cancelOrder(Long orderId) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Order with ID "
                                        + orderId
                                        + " not found."
                        )
                );

        if ("PAID".equals(order.getPaymentStatus())) {

            System.out.println(
                    "Order " + orderId
                            + " was already paid. Initiating refund..."
            );

            Order finalOrder = order;
            PaymentProvider provider = allActiveProviders.stream()
                    .filter(p ->
                            p.getName().equals(
                                    finalOrder.getPaymentMethod()
                            )
                    )
                    .findFirst()
                    .orElse(defaultPrimaryProvider);

            boolean refundSuccess =
                    provider.refundPayment(order.getTotalAmount());

            if (refundSuccess) {

                order.setPaymentStatus("REFUNDED");

                System.out.println(
                        "Refund successful for Order ID: "
                                + orderId
                );

                order = orderRepo.save(order);

                /*
                 * Nach erfolgreichem Commit reagiert der
                 * OrderEventListener und sendet die
                 * Stornierungs-/Refund-E-Mail.
                 */
                eventPublisher.publishEvent(
                        new CancelOrderEvent(order.getId())
                );

            } else {

                order.setPaymentStatus("REFUND_FAILED");

                System.err.println(
                        "Refund failed for Order ID: "
                                + orderId
                );

                order = orderRepo.save(order);
            }

        } else {

            order.setPaymentStatus("CANCELLED");
            order = orderRepo.save(order);

            /*
             * Diese Zeile hinzufügen, falls auch bei einer
             * nicht bezahlten Order eine Stornierungs-E-Mail
             * gesendet werden soll.
             */
            eventPublisher.publishEvent(
                    new CancelOrderEvent(order.getId())
            );
        }

        return order;
    }

    /**
     * Diese Methode wird vom OrderEventListener nach dem Commit aufgerufen.
     *
     * Die neue Read-only-Transaktion hält den Persistence Context offen,
     * während User und Products geladen werden.
     */
    @Transactional(readOnly = true)
    public Order getOrderByIdWithDetails(Long id) {

        Order order = orderRepo.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Order with ID "
                                        + id
                                        + " not found."
                        )
                );

        /*
         * Lazy-Beziehungen innerhalb der laufenden Transaktion laden.
         * Dadurch kann der Listener danach auf User und Products zugreifen.
         */
        order.getUser().getEmail();
        order.getProducts().size();

        return order;
    }
}

