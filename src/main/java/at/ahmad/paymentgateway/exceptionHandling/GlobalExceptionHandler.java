package at.ahmad.paymentgateway.exceptionHandling;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleNotFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Resource not found",
                "message", e.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation failed",
                "message", errorMessage != null ? errorMessage : "Invalid input data"
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalErrors(Exception e) {
        return ResponseEntity.internalServerError().body(Map.of(
                "error", "Internal Server Error",
                "message", "An unexpected error occurred: " + e.getMessage()
        ));
    }
}
