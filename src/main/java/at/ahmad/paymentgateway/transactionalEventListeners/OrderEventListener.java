package at.ahmad.paymentgateway.transactionalEventListeners;

import at.ahmad.paymentgateway.model.Order;
import at.ahmad.paymentgateway.service.EmailService;
import at.ahmad.paymentgateway.service.InvoiceService;
import at.ahmad.paymentgateway.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final EmailService emailService;
    private final InvoiceService invoiceService;
    private final OrderService orderService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleCheckoutEvent(CheckoutEvent event) {

        System.out.println(
                "[EVENT] Order committed successfully. Generating PDF invoice..."
        );

        Order order =
                orderService.getOrderByIdWithDetails(event.id());

        byte[] pdfBytes =
                invoiceService.generateInvoicePdf(order);

        emailService.sendInvoiceEmail(
                order.getUser().getEmail(),
                pdfBytes
        );

        System.out.println(
                "[EVENT] Checkout invoice email sent to "
                        + order.getUser().getEmail()
        );
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleCancelOrderEvent(CancelOrderEvent event) {

        System.out.println(
                "[EVENT] Order cancellation committed successfully. "
                        + "Generating cancellation PDF..."
        );

        Order order =
                orderService.getOrderByIdWithDetails(event.id());

        byte[] pdfBytes =
                invoiceService.generateInvoicePdf(order);

        emailService.sendCancellationEmail(
                order.getUser().getEmail(),
                pdfBytes
        );

        System.out.println(
                "[EVENT] Cancellation email sent to "
                        + order.getUser().getEmail()
        );
    }
}
