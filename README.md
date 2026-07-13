# Payment Gateway Integration Service

A Spring Boot backend application that demonstrates a clean and flexible payment-routing architecture. The project manages users, products, orders, payment-provider selection, PDF invoice generation, invoice delivery by email, and order cancellation or refund workflows.

> **Important:** This project focuses on demonstrating Spring Framework concepts such as Dependency Injection, Profiles, Bean Resolution, Strategy Pattern, Transaction Management, and clean service architecture. Payment providers are intentionally simulated to showcase the architecture rather than integrate with real payment gateways.


## Configuration Before Running the Application

Sensitive infrastructure values are intentionally not included in the repository. Before starting the application, open application.yaml then replace the placeholder values with your own configuration.


### Shop and Invoice Configuration

The company name and currency printed on generated invoices can also be changed.


## Spring Profiles: Development and Production Modes

The active profile is selected in `application.yaml`:

```yaml
spring:
  profiles:
    active: prod
```

Change this value depending on the provider configuration you want to test.

### Non-Production Mode

The `MockPaymentProvider` is annotated with:

```java
@Profile("!prod")
@Component("mock")
@Primary
```

It is active whenever the `prod` profile is **not** active. The provider simulates successful payments locally and does not require a connection to an external payment service.

For example:

```yaml
spring:
  profiles:
    active: dev
```

In this mode, checkout requests are automatically routed to the mock provider, even when another payment method is included in the request.

### Production Profile

The production providers are activated with:

```java
@Profile("prod")
```

The application currently provides:

- `StripeProvider`
- `PayPalProvider`

`StripeProvider` is marked as `@Primary`, so it becomes Spring's default `PaymentProvider` bean when no payment method is explicitly requested.

Although the profile is named `prod`, both providers currently contain simulated payment logic. Official Stripe or PayPal SDK integration can be added later without changing the `OrderService` contract because both implementations already use the common `PaymentProvider` interface.

---

## Project Architecture

### User and Product APIs

`UserController` and `ProductController` provide REST endpoints for creating, reading, listing, and deleting users and products.

The controllers delegate business logic to `UserService` and `ProductService`, while Spring Data repositories handle persistence. Request DTOs and Bean Validation are used to validate incoming data before it reaches the service layer.

The project also includes a centralized `GlobalExceptionHandler` based on `@RestControllerAdvice`.

---

## Payment Provider Architecture

### Common Provider Contract

All payment implementations follow the same interface

This interface separates the order workflow from provider-specific implementation details. `OrderService` works with the `PaymentProvider` abstraction rather than depending directly on Stripe, PayPal, or the mock implementation.

This makes the architecture easier to extend. A new provider can be introduced by implementing `PaymentProvider` and registering the implementation as a Spring bean.

### Bean Construction and Dependency Injection

The active payment implementations are registered as Spring beans through component scanning:

```java
@Component("stripe")
@Component("paypal")
@Component("mock")
```

Their availability is controlled through `@Profile`.

`OrderService` receives both a single provider and a collection of all active providers through constructor injection:


Spring resolves these dependencies automatically:

- `PaymentProvider defaultPrimaryProvider` receives the active bean marked with `@Primary`.
- `List<PaymentProvider> allActiveProviders` receives every active bean that implements `PaymentProvider`.

This demonstrates two important Spring Core features at the same time:

1. selecting one default implementation from multiple candidates
2. injecting all implementations of an interface as a collection

The project also contains a commented `PaymentConfig` class that demonstrates an alternative bean-construction approach with `@Configuration` and `@Bean`. This can be used instead of placing `@Component` and `@Profile` directly on provider classes, but both approaches should not be enabled for the same providers simultaneously.

## PDF Invoice Generation and Email Delivery

After a successful payment or refund, `InvoiceService` delegates invoice creation to a custom `PdfGenerator` bean.

`InvoiceConfig` reads the shop name and currency from `application.yaml` and uses them to construct the generator:

## OrderService and Transaction Management

`OrderService` coordinates the complete checkout and cancellation workflows.

### Checkout Flow

The `checkout` method:

1. loads the requested user
2. loads the requested products
3. calculates the total amount
4. creates an order with the status `PENDING`
5. resolves the appropriate payment provider
6. processes the payment
7. updates the order to `PAID` or `FAILED`
8. publishes a `CheckoutEvent` after a successful payment
9. commits the transaction
10. generates the PDF invoice and sends the email through a transactional event listener

### Cancellation and Refund Flow

The `cancelOrder` method loads an existing order and evaluates its current status.

- A paid order triggers `refundPayment(...)` through the provider stored on the order.
- A successful refund changes the status to `REFUNDED`.
- A failed refund changes the status to `REFUND_FAILED`.
- An unpaid order is changed to `CANCELLED`.

Orders are updated instead of being deleted, preserving their history in the database.

Both checkout and cancellation are annotated with:

```java
@Transactional
```
füge diesen Abschnitt ein:

````md
### Transactional Event Processing

The project uses Spring's event mechanism to separate the order workflow from post-processing tasks.

After a successful checkout or cancellation, `OrderService` publishes a domain event using `ApplicationEventPublisher`.

`OrderEventListener` listens for these events with:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
````

---

## Testing the API

The repository includes:

```text
generated-requests.http
```

This file contains ready-to-run HTTP requests for testing the REST API directly from an IDE that supports HTTP client files, such as IntelliJ IDEA.
Run the requests in order so that the required database records exist before creating an order:

### Receiving the Invoice by Email

In the user-creation request, replace the example email address with your own real email address:

```http
### Create User 1
POST http://localhost:8080/users
Content-Type: application/json

{
  "name": "Felix",
  "email": "your-real-email@gmail.com"
}
```

The checkout request must use the ID of the user whose email address should receive the invoice:

```http
### Checkout
POST http://localhost:8080/orders/checkout
Content-Type: application/json

{
  "userId": 1,
  "productIds": [4, 3, 2],
  "paymentMethod": "paypal"
}
```

For example, when your real email address belongs to user ID `3`, change the request to:

```json
{
  "userId": 3,
  "productIds": [4, 3, 2],
  "paymentMethod": "paypal"
}
```

The `productIds` must also match products that exist in your database.

To refund or cancel an order, use the ID returned by the checkout response:

```http
### Refund or Cancel
POST http://localhost:8080/orders/cancel/1
```

Replace `1` with the actual order ID when necessary.
