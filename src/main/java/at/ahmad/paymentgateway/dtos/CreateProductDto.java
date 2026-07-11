package at.ahmad.paymentgateway.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateProductDto(
        @NotBlank(message = "Product title cannot be empty")
        String title,

        @Positive(message = "Price must be greater than zero")
        double price
) {}
