package at.ahmad.paymentgateway.controller;

import at.ahmad.paymentgateway.model.Order;
import at.ahmad.paymentgateway.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<Order> checkout(@Valid @RequestBody CheckoutRequest request) {
        Order order = orderService.checkout(
            request.getUserId(),
            request.getProductIds(),
            request.getPaymentMethod()
        );
        return ResponseEntity.ok(order);
    }

    @PostMapping("/cancel/{id}")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long id) {
        Order cancelledOrder = orderService.cancelOrder(id);
        return ResponseEntity.ok(cancelledOrder);
    }

    @Data
    public static class CheckoutRequest {

        @NotNull(message = "User ID cannot be empty.")
        private Long userId;

        @NotEmpty(message = "Produkt-IDs cannot be empty")
        private List<Long> productIds;

        private String paymentMethod;
    }
}
