package at.ahmad.paymentgateway.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

public record CreateUserDto (
    @NotBlank
    String name,
    @Email(message = "The email address is invalid")
    String email
) {}
